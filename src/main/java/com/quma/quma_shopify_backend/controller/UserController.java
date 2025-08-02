package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.ResetPasswordRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.UserRequestDTO;
import com.quma.quma_shopify_backend.services.implementations.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRequestDTO requestDTO, HttpServletResponse httpServletResponse) {
        userService.registerUser(requestDTO, httpServletResponse);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserRequestDTO requestDTO, HttpServletResponse response) {
        userService.login(requestDTO, response);
        return ResponseEntity.ok("Logged in successfully");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        userService.logout(request, response);
        return ResponseEntity.ok("Logged out");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO requestDTO) {
        userService.resetPassword(requestDTO);
        return ResponseEntity.ok().build();
    }
}

