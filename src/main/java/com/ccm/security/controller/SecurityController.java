package com.ccm.security.controller;

import com.ccm.security.pojos.LoginRequest;
import com.ccm.security.pojos.UsersPojo;
import com.ccm.security.responsepojos.LoginResponse;
import com.ccm.security.responsepojos.UsersResponse;
import com.ccm.security.service.SecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("users")
public class SecurityController {
    
    private final SecurityService securityService;
    
    public SecurityController(SecurityService securityService) {
        this.securityService = securityService;
    }

    @GetMapping("check")
    public ResponseEntity<String> check() {
        return new ResponseEntity<>("OK from CCM-Security", HttpStatus.OK);
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(
            @RequestHeader(name = "client_id") String clientId,
            @RequestBody LoginRequest loginRequest) {
        return new ResponseEntity<>(securityService.userLogin(clientId, loginRequest), HttpStatus.OK);
    }

    @PostMapping("createUser")
    public ResponseEntity<String> createUser(@RequestBody UsersPojo usersPojo) {
        return new ResponseEntity<>(securityService.createUser(usersPojo), HttpStatus.OK);
    }

    @GetMapping("logout")
    public ResponseEntity<Map<String, String>> logoutUser() {
        return new ResponseEntity<>(securityService.logoutUser(), HttpStatus.OK);
    }

    @GetMapping("refreshJwtToken")
    public ResponseEntity<String> refreshJwtToken() {
        return new ResponseEntity<>(securityService.refreshJwtToken(), HttpStatus.OK);
    }

    @GetMapping("getAllUsers")
    public ResponseEntity<List<UsersResponse>> getAllUsers() {
        return new ResponseEntity<>(securityService.getAllUsers(), HttpStatus.OK);
    }

    @GetMapping("getUserById")
    public ResponseEntity<UsersResponse> getUserById(@RequestParam("id") Integer id) {
        return new ResponseEntity<>(securityService.getUserById(id), HttpStatus.OK);
    }
    
}
