package com.cuttypaws.controller;

import com.cuttypaws.dto.LoginRequest;
import com.cuttypaws.dto.UserDto;
import com.cuttypaws.entity.User;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.impl.DeviceAuthService;
import com.cuttypaws.service.interf.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private final UserService userService;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final DeviceAuthService deviceAuthService;


    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@RequestBody UserDto registrationRequest) {
        UserResponse response = userService.registerUser(registrationRequest);
        if (response.getStatus() == 400) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-company")
    public ResponseEntity<?> registerCompany(@RequestBody UserDto registrationRequest) {
        registrationRequest.setRole("ROLE_COMPANY");
        UserResponse response = userService.registerUser(registrationRequest);
        if (response.getStatus() == 400) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> loginUser (@RequestBody LoginRequest loginRequest, HttpServletRequest request){
        return ResponseEntity.ok(userService.loginUser(loginRequest, request));
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<UserResponse> requestPasswordReset(@RequestParam String email) {
        return ResponseEntity.ok(userService.requestPasswordReset(email));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<UserResponse> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword
    ) {
        return ResponseEntity.ok(userService.resetPassword(token, newPassword));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<UserResponse> resendVerificationCode(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            // Validate user exists and credentials are correct first
            User user = userRepo.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid credentials");
            }

            String deviceId = deviceAuthService.generateDeviceId(request);
            deviceAuthService.sendVerificationCode(user.getEmail(), deviceId);
            String remainingTime = deviceAuthService.getRemainingTime(user.getEmail(), deviceId);

            return ResponseEntity.ok(UserResponse.builder()
                    .status(200)
                    .requiresVerification(true)
                    .remainingTime(remainingTime)
                    .message("New verification code sent to your email")
                    .build());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    UserResponse.builder()
                            .status(400)
                            .message("Failed to resend verification code: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<UserResponse> verifyCode(@RequestBody LoginRequest verifyRequest, HttpServletRequest request) {
        try {
            // This will trigger the verification code processing in UserServiceImpl
            UserResponse response = userService.loginUser(verifyRequest, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    UserResponse.builder()
                            .status(400)
                            .message("Verification failed: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<UserResponse> refreshToken(@RequestParam String refreshToken) {
        try {
            UserResponse response = userService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    UserResponse.builder()
                            .status(400)
                            .message("Token refresh failed: " + e.getMessage())
                            .build()
            );
        }
    }

}





