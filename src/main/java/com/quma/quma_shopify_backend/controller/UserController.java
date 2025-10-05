package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.ResetPasswordRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.UserProfileRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.UserRequestDTO;
import com.quma.quma_shopify_backend.services.implementations.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<?> registerUser(@RequestBody UserRequestDTO requestDTO,
            HttpServletResponse httpServletResponse) {
        Map<String, Object> data = userService.registerUser(requestDTO, httpServletResponse);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserRequestDTO requestDTO, HttpServletResponse response) {
        Map<String, Object> data = userService.login(requestDTO, response);
        return ResponseEntity.ok(data);
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

    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UserProfileRequestDTO profileDTO,
            HttpServletRequest request) {

        userService.updateProfile(request, profileDTO);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @GetMapping("/check-profile-completion")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        Map<String, Object> data = userService.getCurrentUser(request);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request) {
        Map<String, Object> data = userService.getUserProfile(request);
        return ResponseEntity.ok(data);
    }

}
