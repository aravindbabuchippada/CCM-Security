package com.ccm.security.service;

import com.ccm.common.exception.UpdateException;
import com.ccm.common.jwt.JwtUtils;
import com.ccm.common.jwt.TokenContext;
import com.ccm.common.pojo.CCMUser;
import com.ccm.security.entity.Users;
import com.ccm.security.pojos.LoginRequest;
import com.ccm.security.pojos.UsersPojo;
import com.ccm.security.repository.UsersRepository;
import com.ccm.security.responsepojos.LoginResponse;
import com.ccm.security.responsepojos.UsersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class SecurityService {
    private final RedisTemplate<String, String> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UsersRepository usersRepository;

    public SecurityService(@Qualifier("redisTemplate")  RedisTemplate<String, String> redisTemplate,
                         BCryptPasswordEncoder passwordEncoder,
                         JwtUtils jwtUtils,
                         AuthenticationManager authenticationManager,
                         UsersRepository usersRepository) {
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.usersRepository = usersRepository;
    }

    private static final String SECRET_KEY = "U2FsdGVkX19H0qp5uV8zYg=="; // 128-bit Base64
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    /**
     * Authenticating user and creating jwt token for further operations
     */
    public LoginResponse userLogin(String clientId, LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getLoginId(), loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            CCMUser ccmUser = (CCMUser) authentication.getPrincipal();

            // Always generate a new sessionID and new token
            String newSessionId = UUID.randomUUID().toString();
            String newToken = jwtUtils.generateJwtToken(authentication, newSessionId);

            // Store new token in Redis with unique sessionID
            String redisKey = clientId + "::" + loginRequest.getLoginId() + "::" + newSessionId;
            redisTemplate.opsForValue().set(redisKey, newToken, 120, TimeUnit.MINUTES);

            return toLoginResponse(ccmUser, encrypt(newToken));
        } catch (BadCredentialsException e) {
            throw new UpdateException("Invalid Login Credentials.");
        }
    }

    private LoginResponse toLoginResponse(CCMUser ccmUser, String jwtToken) {
        return LoginResponse.builder()
                .loginId(ccmUser.getLoginId())
                .token(jwtToken)
                .build();
    }

    private static String encrypt(String plainText) {
        try {
            byte[] aesKey = Base64.getDecoder().decode(SECRET_KEY); // 16 bytes
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivParams = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParams);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            throw new UpdateException("Encryption failed: " + e.getMessage());
        }
    }

    private String getLoginIdFromAuth(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof CCMUser ccmUser) {
            return ccmUser.getLoginId();
        } else if (principal instanceof String str) {
            return str;
        } else {
            throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
        }
    }

    public Map<String, String> logoutUser() {
        try {
            String loginId = getLoginIdFromAuth();
            String sessionId = jwtUtils.getSessionIdFromJwtToken(TokenContext.getToken());
            String redisKey = loginId+"::"+sessionId;
            String existingToken = redisTemplate.opsForValue().get(redisKey);
            Map<String, String> response = new HashMap<>();

            if(existingToken != null) {
                redisTemplate.delete(redisKey);
                response.put("message", "Logout Successfully !! Token has been removed from Redis.");
                return response;
            } else {
                response.put("message", "No Active session found for the user "+loginId);
                return response;
            }
        } catch (Exception e) {
            throw new NoSuchElementException(e);
        }
    }

    public String refreshJwtToken() {
        String refreshedJwtToken = jwtUtils.refreshJwtTokenIfExpiringSoon();
        return refreshedJwtToken;
    }

    /**
     * Creating user by getting user details input
     */
    public String createUser(UsersPojo usersPojo) {
        Users userDetailsByLoginId = usersRepository.findByLoginId(usersPojo.getLoginId());

        if (!ObjectUtils.isEmpty(userDetailsByLoginId)) {
            throw new UpdateException("User already exist with the login id " + userDetailsByLoginId.getLoginId());
        }
        usersPojo.setPassword(passwordEncoder.encode(usersPojo.getPassword()));

        Users user = new Users();
        BeanUtils.copyProperties(usersPojo, user);
        user.setCreatedOn(LocalDateTime.now());

        try {
            Users savedUser = usersRepository.save(user);

            if (ObjectUtils.isEmpty(savedUser)) {
                throw new InternalError("There is an issue while creating user: " + usersPojo.getLoginId());
            }
            if (logger.isInfoEnabled()) {
                logger.info("user has been created with the user id {} ", usersPojo.getLoginId());
            }

            return "User has been registered";
        } catch (UpdateException e) {
            if (logger.isErrorEnabled()) {
                logger.error("There is an issue while saving details {} rollback has been done", e.getMessage());
            }
            throw new UpdateException("transaction failed rolling back all the data");
        }
    }

    /**
     * This method will fetch all the users available in the system
     */
    public List<UsersResponse> getAllUsers() {
        List<Users> userList = usersRepository.findAll();

        if (CollectionUtils.isEmpty(userList)) {
            throw new UpdateException("No User data found in the system");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Fetched all the user details");
        }
        List<UsersResponse> usersResponseList = new ArrayList<>();
        userList.forEach(user -> {
            UsersResponse usersResponse = new UsersResponse();
            BeanUtils.copyProperties(user, usersResponse);
            usersResponseList.add(usersResponse);
        });

        return usersResponseList;
    }

    /**
     * This method will find the user details by the id of the user
     */
    public UsersResponse getUserById(Integer id) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new UpdateException("There is no data with the id: " + id));
        UsersResponse usersResponse = toUsersResponse(user);
        return usersResponse;
    }

    private UsersResponse toUsersResponse(Users user) {
        UsersResponse usersResponse = new UsersResponse();
        BeanUtils.copyProperties(user, usersResponse);
        return usersResponse;
    }

}
