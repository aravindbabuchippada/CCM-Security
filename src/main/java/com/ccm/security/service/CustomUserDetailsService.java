package com.ccm.security.service;

import com.ccm.common.pojo.CCMUser;
import com.ccm.security.entity.Users;
import com.ccm.security.repository.UsersRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;

    public CustomUserDetailsService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) {

        Users user = usersRepository.findByLoginId(loginId);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return CCMUser.builder()
                .loginId(user.getLoginId())
                .password(user.getPassword())
                .build();
    }
}

