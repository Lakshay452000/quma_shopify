package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.OtpDTO;
import com.quma.quma_shopify_backend.services.AuthService;
import com.quma.quma_shopify_backend.services.implementations.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// AuthController.java
/*
IMPORTANT: No jwt authentication is required for these endpoints.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private OtpService otpService;


    @PostMapping("/new-refresh-token")
    public ResponseEntity<String> refresh(HttpServletRequest request, HttpServletResponse response) {
        boolean refreshed = authService.refreshToken(request, response);
        if (!refreshed) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }
        return ResponseEntity.ok("Token refreshed");
    }

    @GetMapping("/status")
    public ResponseEntity<?> authStatus(HttpServletRequest request) {
        boolean loggedIn = authService.authStatus(request);
        if (loggedIn) {
            return ResponseEntity.ok("Authenticated");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody @Valid OtpDTO otpDTO) {
        otpService.sendOtp(otpDTO);
        return ResponseEntity.ok("OTP sent");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody OtpDTO otpDTO) {
        otpService.verifyOtp(otpDTO);
        return ResponseEntity.ok("OTP verified");
    }

    @GetMapping("/cdn-upload-auth")
    public ResponseEntity<Map<String, Object>> getCdnUploadAuthSignature() throws Exception {
        return ResponseEntity.ok(authService.getCdnUploadAuthSignature());
    }
}