//package com.cuttypaws.service.impl;
//
//import com.cuttypaws.dto.*;
//import com.cuttypaws.entity.*;
//import com.cuttypaws.enums.UserRole;
//import com.cuttypaws.exception.*;
//import com.cuttypaws.mapper.ProductMapper;
//import com.cuttypaws.mapper.UserMapper;
//import com.cuttypaws.repository.*;
//import com.cuttypaws.response.UserResponse;
//import com.cuttypaws.security.InputSanitizer;
//import com.cuttypaws.security.JwtUtils;
//import com.cuttypaws.security.RateLimitService;
//import com.cuttypaws.security.SecurityService;
//import com.cuttypaws.service.AwsS3Service;
//import com.cuttypaws.service.EmailService;
//import com.cuttypaws.service.interf.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.task.TaskExecutor;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.UUID;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class UserServiceImpl implements UserService {
//
//    private final UserRepo userRepo;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtUtils jwtUtils;
//    private final ProductMapper productMapper;
//    private final UserMapper userMapper;
//    private final PasswordResetTokenRepo passwordResetTokenRepo;
//    private final EmailService emailService;
//    private final SecurityService securityService;
//    private final TaskExecutor taskExecutor;
//    private final RateLimitService rateLimitService;
//    private final InputSanitizer inputSanitizer;
//    private final DeviceAuthService deviceAuthService;
//
//    // Constants
//    private static final String STRONG_PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$";
//    private static final int PASSWORD_RESET_EXPIRY_MINUTES = 15;
//    private final AwsS3Service awsS3Service;
//
//    // ============ REGISTRATION ============
//
//    @Override
//    @Transactional
//    public UserResponse registerUser(UserDto registrationRequest) {
//        log.info("Starting user registration for email: {}", registrationRequest.getEmail());
//
//        try {
//            // 1. Input validation and sanitization
//            validateRegistrationInput(registrationRequest);
//
//            // 2. Check if user already exists
//            if (userRepo.findByEmail(registrationRequest.getEmail()).isPresent()) {
//                throw new UserAlreadyExistsException("Email is already registered");
//            }
//            if (userRepo.findByPhoneNumber(registrationRequest.getPhoneNumber()).isPresent()) {
//                throw new UserAlreadyExistsException("Phone number is already registered");
//            }
//
//            // 3. Validate password strength
//            if (!Pattern.matches(STRONG_PASSWORD_REGEX, registrationRequest.getPassword())) {
//                throw new InvalidPasswordException(
//                        "Password must be at least 8 characters with uppercase, lowercase, number and special character"
//                );
//            }
//
//            // 4. Create and save user
//            User user = createUserFromDto(registrationRequest);
//            User savedUser = userRepo.save(user);
//
//            // 5. Send welcome email
//            sendWelcomeEmail(savedUser);
//
//            // 6. Prepare response
//            UserDto userDto = userMapper.mapUserToDtoBasic(savedUser);
//
//            log.info("User registered successfully: {}", savedUser.getEmail());
//            return UserResponse.builder()
//                    .status(200)
//                    .message("Registration successful")
//                    .user(userDto)
//                    .timeStamp(LocalDateTime.now())
//                    .build();
//
//        } catch (UserAlreadyExistsException | InvalidPasswordException e) {
//            log.warn("Registration failed for {}: {}", registrationRequest.getEmail(), e.getMessage());
//            throw e;
//        } catch (Exception e) {
//            log.error("Unexpected error during registration for {}: {}",
//                    registrationRequest.getEmail(), e.getMessage(), e);
//            throw new RuntimeException("Registration failed. Please try again.");
//        }
//    }
//
//    // ============ LOGIN ============
//
//    @Override
//    public UserResponse loginUser(LoginRequest loginRequest, HttpServletRequest request) {
//        String clientIP = securityService.getClientIP(request);
//        String email = loginRequest.getEmail();
//
//        log.info("Login attempt for email: {} from IP: {}", email, clientIP);
//
//        try {
//            // 1. Security checks
//            performSecurityChecks(loginRequest, clientIP);
//
//            // 2. Authenticate user
//            User user = authenticateUser(loginRequest, clientIP);
//
//            // 3. Handle device verification
//            String deviceId = deviceAuthService.generateDeviceId(request);
//
//            // If verification code is provided, verify it
//            if (loginRequest.getVerificationCode() != null && !loginRequest.getVerificationCode().isEmpty()) {
//                return processVerificationCode(user, deviceId, loginRequest.getVerificationCode()
//                        , clientIP, request, loginRequest.isRememberMe());
//            }
//
//            // If device is not verified, require verification
//            if (!deviceAuthService.isDeviceVerified(user.getEmail(), deviceId)) {
//                deviceAuthService.sendVerificationCode(user.getEmail(), deviceId);
//                String remainingTime = deviceAuthService.getRemainingTime(user.getEmail(), deviceId);
//
//                log.info("Device verification required for: {}", user.getEmail());
//                return UserResponse.builder()
//                        .status(200)
//                        .requiresVerification(true)
//                        .remainingTime(remainingTime)
//                        .message("Verification code sent to your email")
//                        .build();
//            }
//
//            // 4. Complete login
//            return completeLogin(user, clientIP, request, loginRequest.isRememberMe());
//
//        } catch (Exception e) {
//            handleLoginError(e, clientIP, email);
//            throw e;
//        }
//    }
//
//    private UserResponse processVerificationCode(User user, String deviceId, String code,
//                                             String clientIP, HttpServletRequest request, boolean rememberMe) {
//        DeviceAuthService.VerifyResult result = deviceAuthService.verifyDeviceCode(
//                user.getEmail(), deviceId, code
//        );
//
//        if (result.isSuccess()) {
//            // Successful verification - proceed with login
//            return completeLogin(user, clientIP, request,rememberMe);
//        } else {
//            // Verification failed
//            String remainingTime = deviceAuthService.getRemainingTime(user.getEmail(), deviceId);
//
//            return UserResponse.builder()
//                    .status(400)
//                    .requiresVerification(true)
//                    .remainingTime(remainingTime)
//                    .remainingAttempts(result.getRemainingAttempts())
//                    .message(result.getMessage())
//                    .build();
//        }
//    }
//
//    // In your refreshToken method, fix the token validation:
//    @Override
//    public UserResponse refreshToken(String refreshToken) {
//        try {
//            log.info("Refreshing token");
//
//            // ✅ FIX: Use the overloaded method without UserDetails for refresh tokens
//            if (!jwtUtils.isTokenValid(refreshToken)) {
//                throw new InvalidCredentialException("Invalid or expired refresh token");
//            }
//
//            String email = jwtUtils.getUsernameFromToken(refreshToken);
//            User user = userRepo.findByEmail(email)
//                    .orElseThrow(() -> new NotFoundException("User not found"));
//
//            // Check if this was a "remember me" token
//            Boolean rememberMe = jwtUtils.isRememberMeToken(refreshToken);
//            if (rememberMe == null) {
//                rememberMe = false;
//            }
//
//            // Generate new tokens
//            String newAccessToken = jwtUtils.generateAccessToken(user);
//            String newRefreshToken = jwtUtils.generateRefreshToken(user, rememberMe);
//
//            log.info("Token refreshed successfully for user: {} with rememberMe: {}", email, rememberMe);
//
//            return UserResponse.builder()
//                    .status(200)
//                    .token(newAccessToken)
//                    .refreshToken(newRefreshToken)
//                    .expirationTime("15 minutes")
//                    .message("Token refreshed successfully")
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Token refresh failed: {}", e.getMessage());
//            throw new InvalidCredentialException("Token refresh failed: " + e.getMessage());
//        }
//    }
//
//    private UserResponse completeLogin(User user, String clientIP, HttpServletRequest request, boolean rememberMe) {
//        // Send login notification
//        sendLoginNotification(user, clientIP, request);
//
//        // Generate tokens
//        String accessToken = jwtUtils.generateAccessToken(user);
//        String refreshToken = jwtUtils.generateRefreshToken(user, rememberMe);
//        UserDto userDto = userMapper.mapUserToDtoBasic(user);
//
//        // Log security event
//        securityService.logSecurityEvent("LOGIN_SUCCESS", "User logged in successfully", clientIP, user.getEmail());
//
//        log.info("Login successful for user: {}", user.getEmail(), rememberMe);
//
//        return UserResponse.builder()
//                .status(200)
//                .token(accessToken)
//                .refreshToken(refreshToken)
//                .expirationTime("15 minutes")
//                .role(user.getUserRole().name())
//                .user(userDto)
//                .message("Login successful")
//                .build();
//    }
//
//    // ============ PASSWORD RESET ============
//
//    @Override
//    @Transactional
//    public UserResponse requestPasswordReset(String email) {
//        log.info("Password reset request for email: {}", email);
//
//        try {
//            // Validate email
//            if (!inputSanitizer.isValidEmail(email)) {
//                throw new RuntimeException("Invalid email format");
//            }
//
//            // Rate limiting
//            if (rateLimitService.isPasswordResetLimited(email)) {
//                throw new RuntimeException("Too many password reset requests. Please wait 15 minutes.");
//            }
//
//            // Find user
//            User user = userRepo.findByEmail(email)
//                    .orElseThrow(() -> new NotFoundException("User not found"));
//
//            // Clean old tokens
//            passwordResetTokenRepo.deleteByUser(user);
//
//            // Create new token
//            PasswordResetToken resetToken = createPasswordResetToken(user);
//            passwordResetTokenRepo.save(resetToken);
//
//            // Send reset email
//            sendPasswordResetEmail(user, resetToken.getToken());
//
//            // Record attempt
//            rateLimitService.recordAttempt(email, "PASSWORD_RESET");
//
//            log.info("Password reset email sent to: {}", email);
//            return UserResponse.builder()
//                    .status(200)
//                    .message("Password reset link sent to your email")
//                    .timeStamp(LocalDateTime.now())
//                    .build();
//
//        } catch (NotFoundException e) {
//            // Don't reveal that user doesn't exist for security
//            log.info("Password reset requested for non-existent email: {}", email);
//            return UserResponse.builder()
//                    .status(200)
//                    .message("If the email exists, a reset link has been sent")
//                    .build();
//        } catch (Exception e) {
//            log.error("Password reset request failed for {}: {}", email, e.getMessage());
//            throw new RuntimeException("Failed to process password reset request");
//        }
//    }
//
//    @Override
//    @Transactional
//    public UserResponse resetPassword(String token, String newPassword) {
//        log.info("Password reset attempt with token");
//
//        try {
//            // Validate token
//            PasswordResetToken resetToken = passwordResetTokenRepo.findByToken(token)
//                    .orElseThrow(() -> new NotFoundException("Invalid or expired token"));
//
//            validateResetToken(resetToken);
//
//            // Validate new password
//            if (!Pattern.matches(STRONG_PASSWORD_REGEX, newPassword)) {
//                throw new InvalidPasswordException(
//                        "Password must be at least 8 characters with uppercase, lowercase, number and special character"
//                );
//            }
//
//            // Update password
//            User user = resetToken.getUser();
//            user.setPassword(passwordEncoder.encode(newPassword));
//            userRepo.save(user);
//
//            // Mark token as used and schedule deletion
//            markTokenAsUsed(resetToken);
//            scheduleTokenDeletion(resetToken.getId());
//
//            // Send confirmation email
//            sendPasswordResetConfirmationEmail(user);
//
//            log.info("Password reset successful for user: {}", user.getEmail());
//            return UserResponse.builder()
//                    .status(200)
//                    .message("Password reset successfully")
//                    .timeStamp(LocalDateTime.now())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Password reset failed: {}", e.getMessage());
//            throw new RuntimeException("Password reset failed: " + e.getMessage());
//        }
//    }
//
//    // ============ PROFILE MANAGEMENT ============
//
//    @Override
//    public UserResponse updateUserProfile(UserDto userDto) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//
//        try {
//            User user = userRepo.findByEmail(email)
//                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//
//            // Update allowed fields with sanitization
//            if (userDto.getName() != null) {
//                user.setName(inputSanitizer.sanitize(userDto.getName().trim()));
//            }
//            if (userDto.getPhoneNumber() != null) {
//                String phone = inputSanitizer.sanitize(userDto.getPhoneNumber().trim());
//                // Check if phone number is already taken by another user
//                if (!phone.equals(user.getPhoneNumber())) {
//                    userRepo.findByPhoneNumber(phone).ifPresent(existingUser -> {
//                        if (!existingUser.getId().equals(user.getId())) {
//                            throw new UserAlreadyExistsException("Phone number already registered");
//                        }
//                    });
//                }
//                user.setPhoneNumber(phone);
//            }
//
//            User updatedUser = userRepo.save(user);
//            UserDto updatedUserDto = userMapper.mapUserToDtoBasic(updatedUser);
//
//            log.info("Profile updated successfully for user: {}", email);
//            return UserResponse.builder()
//                    .status(200)
//                    .message("Profile updated successfully")
//                    .user(updatedUserDto)
//                    .timeStamp(LocalDateTime.now())
//                    .build();
//
//        } catch (UserAlreadyExistsException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Profile update failed for {}: {}", email, e.getMessage());
//            throw new RuntimeException("Profile update failed");
//        }
//    }
//
//    // ============ USER INFO METHODS ============
//
//    @Override
//    public User getLoginUser() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String email = authentication.getName();
//        return userRepo.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
//    }
//
//    @Override
//    public UserResponse getUserInfoAndOrderHistory() {
//        User user = getLoginUser();
//        UserDto userDto = userMapper.mapUserToDtoPlusAddressAndOrderHistory(user);
//        return UserResponse.builder()
//                .status(200)
//                .user(userDto)
//                .message("User info retrieved successfully")
//                .build();
//    }
//
//    @Override
//    public UserResponse getUserInfoAndOrdersHistory() {
//        User user = getLoginUser();
//        User userWithDetails = userRepo.findByIdWithAddressAndOrders(user.getId())
//                .orElseThrow(() -> new NotFoundException("User not found"));
//        UserDto userDto = userMapper.mapUserToDtoPlusAddressAndOrderHistory(userWithDetails);
//        return UserResponse.builder()
//                .status(200)
//                .user(userDto)
//                .message("User info with orders retrieved successfully")
//                .build();
//    }
//
//    @Override
//    public UserDto getUserByIdWithAddress(Long id) {
//        return userRepo.findByIdWithAddress(id)
//                .map(userMapper::mapUserToDtoBasic)
//                .orElse(null);
//    }
//
//    @Override
//    public UserResponse updateProfileImage(MultipartFile file) {
//        UserResponse res = new UserResponse();
//        try {
//            if (file == null || file.isEmpty()) {
//                res.setStatus(400);
//                res.setMessage("Image file is missing");
//                return res;
//            }
//
//            User user = getLoginUser();
//            String imageUrl = awsS3Service.saveImageToS3(file);
//
//            user.setProfileImageUrl(imageUrl);
//            userRepo.save(user);
//
//            res.setStatus(200);
//            res.setMessage("Profile image updated successfully");
//            res.setUser(userMapper.mapUserToDtoBasic(user));
//            return res;
//        } catch (Exception e) {
//            log.error("Profile image error: {}", e.getMessage());
//            res.setStatus(500);
//            res.setMessage("Profile image error: " + e.getMessage());
//            return res;
//        }
//    }
//
//    public UserResponse updateCoverImage(MultipartFile file) {
//        UserResponse res = new UserResponse();
//        try {
//            if (file == null || file.isEmpty()) {
//                res.setStatus(400);
//                res.setMessage("Image file is missing");
//                return res;
//            }
//
//            User user = getLoginUser();
//            String imageUrl = awsS3Service.saveImageToS3(file);
//
//            user.setCoverImageUrl(imageUrl);
//            userRepo.save(user);
//
//            res.setStatus(200);
//            res.setMessage("Profile image updated successfully");
//            res.setUser(userMapper.mapUserToDtoBasic(user));
//            return res;
//        } catch (Exception e) {
//            log.error("Cover image error: {}", e.getMessage());
//            res.setStatus(500);
//            res.setMessage("Cover image error: " + e.getMessage());
//            return res;
//        }
//    }
//
//    @Override
//    public UserResponse getAllUser() {
//        List<User> users = userRepo.findAll();
//        List<UserDto> userDtos = users.stream()
//                .map(userMapper::mapUserToDtoBasic)
//                .collect(Collectors.toList());
//
//        return UserResponse.builder()
//                .status(200)
//                .message("Users retrieved successfully")
//                .userList(userDtos)
//                .build();
//    }
//
//    @Override
//    public List<UserDto> getAllUsersWithAddress() {
//        return userRepo.findAllWithAddress().stream()
//                .map(userMapper::mapUserToDtoBasic)
//                .collect(Collectors.toList());
//    }
//
//    public List<UserDto> getAllUsersWithAddressAndOrders() {
//        return userRepo.findAllWithAddressAndOrders().stream()
//                .map(userMapper::mapUserToDtoPlusAddressAndOrderHistory)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public UserResponse getAllUsersWithRoleCompany() {
//        List<User> users = userRepo.findAllByRoleCompany();
//        List<UserDto> userDtos = users.stream()
//                .map(userMapper::mapUserToDtoBasic)
//                .collect(Collectors.toList());
//
//        return UserResponse.builder()
//                .status(200)
//                .message("Company users retrieved successfully")
//                .userList(userDtos)
//                .build();
//    }
//
//    @Override
//    public UserResponse getCompanyWithProducts(Long companyId) {
//        User company = userRepo.findCompanyWithProductsById(companyId)
//                .orElseThrow(() -> new NotFoundException("Company not found"));
//
//        UserDto companyDto = userMapper.mapUserToDtoBasic(company);
//        List<ProductDto> productDtos = company.getProducts().stream()
//                .map(productMapper::mapProductToDtoBasic)
//                .collect(Collectors.toList());
//
//        return UserResponse.builder()
//                .status(200)
//                .message("Company with products retrieved successfully")
//                .user(companyDto)
//                .productList(productDtos)
//                .build();
//    }
//
//    // ============ SCHEDULED TASKS ============
//
//    @Scheduled(fixedRate = 3600000) // Run every hour
//    @Transactional
//    public void cleanupExpiredTokens() {
//        LocalDateTime now = LocalDateTime.now();
//        passwordResetTokenRepo.deleteExpiredOrOldUsedTokens(now, now.minusMinutes(10));
//        log.info("Expired tokens cleanup completed at: {}", now);
//    }
//
//    @Async
//    public void scheduleTokenDeletion(Long tokenId) {
//        taskExecutor.execute(() -> {
//            try {
//                Thread.sleep(300000); // Wait 5 minutes before deletion
//                passwordResetTokenRepo.deleteById(tokenId);
//                log.info("Used token {} deleted", tokenId);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                log.error("Token deletion interrupted for token: {}", tokenId);
//            }
//        });
//    }
//
//    // ============ PRIVATE HELPER METHODS ============
//
//    private void validateRegistrationInput(UserDto registrationRequest) {
//        // Sanitize inputs
//        registrationRequest.setName(inputSanitizer.sanitize(registrationRequest.getName().trim()));
//        registrationRequest.setEmail(inputSanitizer.sanitize(registrationRequest.getEmail().trim().toLowerCase()));
//        registrationRequest.setPhoneNumber(inputSanitizer.sanitize(registrationRequest.getPhoneNumber().trim()));
//
//        // Validate email format
//        if (!inputSanitizer.isValidEmail(registrationRequest.getEmail())) {
//            throw new RuntimeException("Invalid email format");
//        }
//
//        // Check for malicious input
//        if (inputSanitizer.isMalicious(registrationRequest.getName()) ||
//                inputSanitizer.isMalicious(registrationRequest.getEmail()) ||
//                inputSanitizer.isMalicious(registrationRequest.getPhoneNumber())) {
//            throw new RuntimeException("Invalid input detected");
//        }
//    }
//
//    private User createUserFromDto(UserDto dto) {
//        UserRole role = determineUserRole(dto.getRole());
//
//        return User.builder()
//                .name(dto.getName())
//                .email(dto.getEmail())
//                .password(passwordEncoder.encode(dto.getPassword()))
//                .phoneNumber(dto.getPhoneNumber())
//                .companyName(dto.getCompanyName())
//                .businessRegistrationNumber(dto.getBusinessRegistrationNumber())
//                .userRole(role)
//                .build();
//    }
//
//    private UserRole determineUserRole(String role) {
//        if (role == null) return UserRole.ROLE_USER;
//
//        return switch (role.toLowerCase()) {
//            case "role_admin" -> UserRole.ROLE_ADMIN;
//            case "role_customer_support" -> UserRole.ROLE_CUSTOMER_SUPPORT;
//            case "role_company" -> UserRole.ROLE_COMPANY;
//            default -> UserRole.ROLE_USER;
//        };
//    }
//
//    private void performSecurityChecks(LoginRequest loginRequest, String clientIP) {
//        // Rate limiting
//        if (rateLimitService.isLoginLimited(loginRequest.getEmail())) {
//            securityService.logSecurityEvent("RATE_LIMIT_LOGIN",
//                    "Too many login attempts", clientIP, loginRequest.getEmail());
//            throw new RuntimeException("Too many login attempts. Please try again later.");
//        }
//
//        // Sanitize inputs
//        loginRequest.setEmail(inputSanitizer.sanitize(loginRequest.getEmail().trim().toLowerCase()));
//
//        // Check for malicious input
//        if (inputSanitizer.isMalicious(loginRequest.getEmail()) ||
//                inputSanitizer.isMalicious(loginRequest.getPassword())) {
//            securityService.logSecurityEvent("MALICIOUS_LOGIN",
//                    "Malicious login input", clientIP, loginRequest.getEmail());
//            throw new RuntimeException("Invalid credentials");
//        }
//
//        // Check if IP is blocked
//        if (securityService.isIpBlocked(clientIP)) {
//            securityService.logSecurityEvent("BLOCKED_IP_LOGIN",
//                    "Blocked IP login attempt", clientIP, loginRequest.getEmail());
//            throw new RuntimeException("Access denied");
//        }
//
//        // Record attempt
//        rateLimitService.recordAttempt(loginRequest.getEmail(), "LOGIN");
//    }
//
//    private User authenticateUser(LoginRequest loginRequest, String clientIP) {
//        User user = userRepo.findByEmail(loginRequest.getEmail())
//                .orElseThrow(() -> {
//                    securityService.logSecurityEvent("LOGIN_FAILED",
//                            "Email not found", clientIP, loginRequest.getEmail());
//                    return new NotFoundException("Invalid email or password");
//                });
//
//        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
//            securityService.logSecurityEvent("LOGIN_FAILED",
//                    "Wrong password", clientIP, loginRequest.getEmail());
//            throw new InvalidCredentialException("Invalid email or password");
//        }
//
//        return user;
//    }
//
//    private void handleLoginError(Exception e, String clientIP, String email) {
//        if (!(e.getMessage().contains("verification") || e.getMessage().contains("code"))) {
//            securityService.logSecurityEvent("LOGIN_ERROR",
//                    "Login error: " + e.getMessage(), clientIP, email);
//        }
//        log.error("Login failed for email: {} - Error: {}", email, e.getMessage());
//    }
//
//    private void sendLoginNotification(User user, String clientIP, HttpServletRequest request) {
//        try {
//            String deviceInfo = deviceAuthService.getDeviceInfo(request);
//            String location = securityService.getSimpleLocation(clientIP);
//            deviceAuthService.sendLoginNotification(user, deviceInfo, location, request);
//        } catch (Exception e) {
//            log.warn("Failed to send login notification: {}", e.getMessage());
//            // Don't fail login if notification fails
//        }
//    }
//
//    private PasswordResetToken createPasswordResetToken(User user) {
//        PasswordResetToken token = new PasswordResetToken();
//        token.setToken(UUID.randomUUID().toString());
//        token.setUser(user);
//        token.setExpiryDate(LocalDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRY_MINUTES));
//        token.setUsed(false);
//        return token;
//    }
//
//    private void validateResetToken(PasswordResetToken token) {
//        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
//            passwordResetTokenRepo.delete(token);
//            throw new TokenExpiredException("Reset token has expired");
//        }
//        if (token.isUsed()) {
//            throw new InvalidCredentialException("This reset link has already been used");
//        }
//    }
//
//    private void markTokenAsUsed(PasswordResetToken token) {
//        token.setUsed(true);
//        token.setUsedAt(LocalDateTime.now());
//        passwordResetTokenRepo.save(token);
//    }
//
//    private void sendWelcomeEmail(User user) {
//        emailService.sendEmail(
//                user.getEmail(),
//                "Welcome to KinjoMarket!",
//                "Dear " + user.getName() + ",\n\nThank you for registering with KinjoMarket!"
//        );
//    }
//
//    private void sendPasswordResetEmail(User user, String token) {
//        String resetLink = "https://www.kinjomarket.com/reset-password?token=" + token;
//        String body = String.format(
//                "Dear %s,\n\n" +
//                        "You requested a password reset. Click the link below (valid for %d minutes):\n%s\n\n" +
//                        "If you didn't request this, please ignore this email.\n\n" +
//                        "Best regards,\nKinjoMarket Team",
//                user.getName(), PASSWORD_RESET_EXPIRY_MINUTES, resetLink
//        );
//
//        emailService.sendEmail(user.getEmail(), "Password Reset Request", body);
//    }
//
//    private void sendPasswordResetConfirmationEmail(User user) {
//        emailService.sendEmail(
//                user.getEmail(),
//                "Password Reset Successful",
//                "Your password has been successfully reset. If you didn't make this change, please contact support immediately."
//        );
//    }
//}








package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.enums.UserRole;
import com.cuttypaws.exception.*;
import com.cuttypaws.mapper.ProductMapper;
import com.cuttypaws.mapper.UserMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.security.InputSanitizer;
import com.cuttypaws.security.JwtUtils;
import com.cuttypaws.security.RateLimitService;
import com.cuttypaws.security.SecurityService;
import com.cuttypaws.service.AwsS3Service;
import com.cuttypaws.service.EmailService;
import com.cuttypaws.service.interf.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final PasswordResetTokenRepo passwordResetTokenRepo;
    private final EmailService emailService;
    private final SecurityService securityService;

    @Qualifier("tokenTaskScheduler")
    private final TaskScheduler taskScheduler;

    private final RateLimitService rateLimitService;
    private final InputSanitizer inputSanitizer;
    private final DeviceAuthService deviceAuthService;
    private final AwsS3Service awsS3Service;


    public UserServiceImpl(
            UserRepo userRepo,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            ProductMapper productMapper,
            UserMapper userMapper,
            PasswordResetTokenRepo passwordResetTokenRepo,
            EmailService emailService,
            SecurityService securityService,
            @Qualifier("tokenTaskScheduler") TaskScheduler taskScheduler,
            RateLimitService rateLimitService,
            InputSanitizer inputSanitizer,
            DeviceAuthService deviceAuthService,
            AwsS3Service awsS3Service
    ) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.passwordResetTokenRepo = passwordResetTokenRepo;
        this.emailService = emailService;
        this.securityService = securityService;
        this.taskScheduler = taskScheduler;
        this.rateLimitService = rateLimitService;
        this.inputSanitizer = inputSanitizer;
        this.deviceAuthService = deviceAuthService;
        this.awsS3Service = awsS3Service;
    }

    // Constants
    private static final String STRONG_PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$";
    private static final int PASSWORD_RESET_EXPIRY_MINUTES = 15;

    // ============ REGISTRATION ============

    @Override
    @Transactional
    public UserResponse registerUser(UserDto registrationRequest) {
        log.info("Starting user registration for email: {}", registrationRequest.getEmail());

        try {
            // 1. Input validation and sanitization
            validateRegistrationInput(registrationRequest);

            // 2. Check if user already exists
            if (userRepo.findByEmail(registrationRequest.getEmail()).isPresent()) {
                throw new UserAlreadyExistsException("Email is already registered");
            }
            if (userRepo.findByPhoneNumber(registrationRequest.getPhoneNumber()).isPresent()) {
                throw new UserAlreadyExistsException("Phone number is already registered");
            }

            // 3. Validate password strength
            if (!Pattern.matches(STRONG_PASSWORD_REGEX, registrationRequest.getPassword())) {
                throw new InvalidPasswordException(
                        "Password must be at least 8 characters with uppercase, lowercase, number and special character"
                );
            }

            // 4. Create and save user
            User user = createUserFromDto(registrationRequest);
            User savedUser = userRepo.save(user);

            // 5. Send welcome email
            sendWelcomeEmail(savedUser);

            // 6. Prepare response
            UserDto userDto = userMapper.mapUserToDtoBasic(savedUser);

            log.info("User registered successfully: {}", savedUser.getEmail());
            return UserResponse.builder()
                    .status(200)
                    .message("Registration successful")
                    .user(userDto)
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (UserAlreadyExistsException | InvalidPasswordException e) {
            log.warn("Registration failed for {}: {}", registrationRequest.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during registration for {}: {}",
                    registrationRequest.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Registration failed. Please try again.");
        }
    }

    // ============ LOGIN ============

    @Override
    public UserResponse loginUser(LoginRequest loginRequest, HttpServletRequest request) {
        String clientIP = securityService.getClientIP(request);
        String email = loginRequest.getEmail();

        log.info("Login attempt for email: {} from IP: {}", email, clientIP);

        try {
            // 1. Security checks
            performSecurityChecks(loginRequest, clientIP);

            // 2. Authenticate user
            User user = authenticateUser(loginRequest, clientIP);

            // 3. Handle device verification
            String deviceId = deviceAuthService.generateDeviceId(request);

            // If verification code is provided, verify it
            if (loginRequest.getVerificationCode() != null && !loginRequest.getVerificationCode().isEmpty()) {
                return processVerificationCode(user, deviceId, loginRequest.getVerificationCode(),
                        clientIP, request, loginRequest.isRememberMe());
            }

            // If device is not verified, require verification
            if (!deviceAuthService.isDeviceVerified(user.getEmail(), deviceId)) {
                deviceAuthService.sendVerificationCode(user.getEmail(), deviceId);
                String remainingTime = deviceAuthService.getRemainingTime(user.getEmail(), deviceId);

                log.info("Device verification required for: {}", user.getEmail());
                return UserResponse.builder()
                        .status(200)
                        .requiresVerification(true)
                        .remainingTime(remainingTime)
                        .message("Verification code sent to your email")
                        .build();
            }

            // 4. Complete login
            return completeLogin(user, clientIP, request, loginRequest.isRememberMe());

        } catch (Exception e) {
            handleLoginError(e, clientIP, email);
            throw e;
        }
    }

    private UserResponse processVerificationCode(User user, String deviceId, String code,
                                                 String clientIP, HttpServletRequest request, boolean rememberMe) {
        DeviceAuthService.VerifyResult result = deviceAuthService.verifyDeviceCode(
                user.getEmail(), deviceId, code
        );

        if (result.isSuccess()) {
            // Successful verification - proceed with login
            return completeLogin(user, clientIP, request, rememberMe);
        } else {
            // Verification failed
            String remainingTime = deviceAuthService.getRemainingTime(user.getEmail(), deviceId);

            return UserResponse.builder()
                    .status(400)
                    .requiresVerification(true)
                    .remainingTime(remainingTime)
                    .remainingAttempts(result.getRemainingAttempts())
                    .message(result.getMessage())
                    .build();
        }
    }

    // In your refreshToken method, fix the token validation:
    @Override
    public UserResponse refreshToken(String refreshToken) {
        try {
            log.info("Refreshing token");

            // ✅ FIX: Use the overloaded method without UserDetails for refresh tokens
            if (!jwtUtils.isTokenValid(refreshToken)) {
                throw new InvalidCredentialException("Invalid or expired refresh token");
            }

            String email = jwtUtils.getUsernameFromToken(refreshToken);
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            // Check if this was a "remember me" token
            Boolean rememberMe = jwtUtils.isRememberMeToken(refreshToken);
            if (rememberMe == null) {
                rememberMe = false;
            }

            // Generate new tokens
            String newAccessToken = jwtUtils.generateAccessToken(user);
            String newRefreshToken = jwtUtils.generateRefreshToken(user, rememberMe);

            log.info("Token refreshed successfully for user: {} with rememberMe: {}", email, rememberMe);

            return UserResponse.builder()
                    .status(200)
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expirationTime("15 minutes")
                    .message("Token refreshed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new InvalidCredentialException("Token refresh failed: " + e.getMessage());
        }
    }

    private UserResponse completeLogin(User user, String clientIP, HttpServletRequest request, boolean rememberMe) {
        // Send login notification
        sendLoginNotification(user, clientIP, request);

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user, rememberMe);
        UserDto userDto = userMapper.mapUserToDtoBasic(user);

        // Log security event
        securityService.logSecurityEvent("LOGIN_SUCCESS", "User logged in successfully", clientIP, user.getEmail());

        log.info("Login successful for user: {}", user.getEmail(), rememberMe);

        return UserResponse.builder()
                .status(200)
                .token(accessToken)
                .refreshToken(refreshToken)
                .expirationTime("15 minutes")
                .role(user.getUserRole().name())
                .user(userDto)
                .message("Login successful")
                .build();
    }

    // ============ PASSWORD RESET ============

    @Override
    @Transactional
    public UserResponse requestPasswordReset(String email) {
        log.info("Password reset request for email: {}", email);

        try {
            // Validate email
            if (!inputSanitizer.isValidEmail(email)) {
                throw new RuntimeException("Invalid email format");
            }

            // Rate limiting
            if (rateLimitService.isPasswordResetLimited(email)) {
                throw new RuntimeException("Too many password reset requests. Please wait 15 minutes.");
            }

            // Find user
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            // Clean old tokens
            passwordResetTokenRepo.deleteByUser(user);

            // Create new token
            PasswordResetToken resetToken = createPasswordResetToken(user);
            passwordResetTokenRepo.save(resetToken);

            // Send reset email
            sendPasswordResetEmail(user, resetToken.getToken());

            // Record attempt
            rateLimitService.recordAttempt(email, "PASSWORD_RESET");

            log.info("Password reset email sent to: {}", email);
            return UserResponse.builder()
                    .status(200)
                    .message("Password reset link sent to your email")
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (NotFoundException e) {
            // Don't reveal that user doesn't exist for security
            log.info("Password reset requested for non-existent email: {}", email);
            return UserResponse.builder()
                    .status(200)
                    .message("If the email exists, a reset link has been sent")
                    .build();
        } catch (Exception e) {
            log.error("Password reset request failed for {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to process password reset request");
        }
    }

    @Override
    @Transactional
    public UserResponse resetPassword(String token, String newPassword) {
        log.info("Password reset attempt with token");

        try {
            // Validate token
            PasswordResetToken resetToken = passwordResetTokenRepo.findByToken(token)
                    .orElseThrow(() -> new NotFoundException("Invalid or expired token"));

            validateResetToken(resetToken);

            // Validate new password
            if (!Pattern.matches(STRONG_PASSWORD_REGEX, newPassword)) {
                throw new InvalidPasswordException(
                        "Password must be at least 8 characters with uppercase, lowercase, number and special character"
                );
            }

            // Update password
            User user = resetToken.getUser();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepo.save(user);

            // Mark token as used and schedule deletion (non-blocking, preserves 5-minute behavior)
            markTokenAsUsed(resetToken);
            scheduleTokenDeletion(resetToken.getId());

            // Send confirmation email
            sendPasswordResetConfirmationEmail(user);

            log.info("Password reset successful for user: {}", user.getEmail());
            return UserResponse.builder()
                    .status(200)
                    .message("Password reset successfully")
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            throw new RuntimeException("Password reset failed: " + e.getMessage());
        }
    }

    // ============ PROFILE MANAGEMENT ============

    @Override
    public UserResponse updateUserProfile(UserDto userDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        try {
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Update allowed fields with sanitization
            if (userDto.getName() != null) {
                user.setName(inputSanitizer.sanitize(userDto.getName().trim()));
            }
            if (userDto.getPhoneNumber() != null) {
                String phone = inputSanitizer.sanitize(userDto.getPhoneNumber().trim());
                // Check if phone number is already taken by another user
                if (!phone.equals(user.getPhoneNumber())) {
                    userRepo.findByPhoneNumber(phone).ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(user.getId())) {
                            throw new UserAlreadyExistsException("Phone number already registered");
                        }
                    });
                }
                user.setPhoneNumber(phone);
            }

            User updatedUser = userRepo.save(user);
            UserDto updatedUserDto = userMapper.mapUserToDtoBasic(updatedUser);

            log.info("Profile updated successfully for user: {}", email);
            return UserResponse.builder()
                    .status(200)
                    .message("Profile updated successfully")
                    .user(updatedUserDto)
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (UserAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Profile update failed for {}: {}", email, e.getMessage());
            throw new RuntimeException("Profile update failed");
        }
    }

    // ============ USER INFO METHODS ============

    @Override
    public User getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public UserResponse getUserInfoAndOrderHistory() {
        User user = getLoginUser();
        UserDto userDto = userMapper.mapUserToDtoPlusAddressAndOrderHistory(user);
        return UserResponse.builder()
                .status(200)
                .user(userDto)
                .message("User info retrieved successfully")
                .build();
    }

    @Override
    public UserResponse getUserInfoAndOrdersHistory() {
        User user = getLoginUser();
        User userWithDetails = userRepo.findByIdWithAddressAndOrders(user.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        UserDto userDto = userMapper.mapUserToDtoPlusAddressAndOrderHistory(userWithDetails);
        return UserResponse.builder()
                .status(200)
                .user(userDto)
                .message("User info with orders retrieved successfully")
                .build();
    }

    @Override
    public UserDto getUserByIdWithAddress(UUID id) {
        return userRepo.findByIdWithAddress(id)
                .map(userMapper::mapUserToDtoBasic)
                .orElse(null);
    }

    @Override
    public UserResponse updateProfileImage(MultipartFile file) {
        UserResponse res = new UserResponse();
        try {
            if (file == null || file.isEmpty()) {
                res.setStatus(400);
                res.setMessage("Image file is missing");
                return res;
            }

            User user = getLoginUser();
            String imageUrl = awsS3Service.uploadMedia(file);

            user.setProfileImageUrl(imageUrl);
            userRepo.save(user);

            res.setStatus(200);
            res.setMessage("Profile image updated successfully");
            res.setUser(userMapper.mapUserToDtoBasic(user));
            return res;
        } catch (Exception e) {
            log.error("Profile image error: {}", e.getMessage());
            res.setStatus(500);
            res.setMessage("Profile image error: " + e.getMessage());
            return res;
        }
    }

    public UserResponse updateCoverImage(MultipartFile file) {
        UserResponse res = new UserResponse();
        try {
            if (file == null || file.isEmpty()) {
                res.setStatus(400);
                res.setMessage("Image file is missing");
                return res;
            }

            User user = getLoginUser();
            String imageUrl = awsS3Service.uploadMedia(file);

            user.setCoverImageUrl(imageUrl);
            userRepo.save(user);

            res.setStatus(200);
            res.setMessage("Profile image updated successfully");
            res.setUser(userMapper.mapUserToDtoBasic(user));
            return res;
        } catch (Exception e) {
            log.error("Cover image error: {}", e.getMessage());
            res.setStatus(500);
            res.setMessage("Cover image error: " + e.getMessage());
            return res;
        }
    }

    @Override
    public UserResponse getAllUser() {
        List<User> users = userRepo.findAll();
        List<UserDto> userDtos = users.stream()
                .map(userMapper::mapUserToDtoBasic)
                .collect(Collectors.toList());

        return UserResponse.builder()
                .status(200)
                .message("Users retrieved successfully")
                .userList(userDtos)
                .build();
    }

    @Override
    public List<UserDto> getAllUsersWithAddress() {
        return userRepo.findAllWithAddress().stream()
                .map(userMapper::mapUserToDtoBasic)
                .collect(Collectors.toList());
    }

    public List<UserDto> getAllUsersWithAddressAndOrders() {
        return userRepo.findAllWithAddressAndOrders().stream()
                .map(userMapper::mapUserToDtoPlusAddressAndOrderHistory)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getAllUsersWithRoleCompany() {
        List<User> users = userRepo.findAllByRoleCompany();
        List<UserDto> userDtos = users.stream()
                .map(userMapper::mapUserToDtoBasic)
                .collect(Collectors.toList());

        return UserResponse.builder()
                .status(200)
                .message("Company users retrieved successfully")
                .userList(userDtos)
                .build();
    }

    @Override
    public UserResponse getCompanyWithProducts(UUID companyId) {
        User company = userRepo.findCompanyWithProductsById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        UserDto companyDto = userMapper.mapUserToDtoBasic(company);
        List<ProductDto> productDtos = company.getProducts().stream()
                .map(productMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());

        return UserResponse.builder()
                .status(200)
                .message("Company with products retrieved successfully")
                .user(companyDto)
                .productList(productDtos)
                .build();
    }

    // ============ SCHEDULED TASKS ============

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        passwordResetTokenRepo.deleteExpiredOrOldUsedTokens(now, now.minusMinutes(10));
        log.info("Expired tokens cleanup completed at: {}", now);
    }


    public void scheduleTokenDeletion(Long tokenId) {
        Instant runAt = Instant.now().plusSeconds(300); // 5 minutes
        taskScheduler.schedule(() -> {
            try {
                passwordResetTokenRepo.deleteById(tokenId);
                log.info("Used token {} deleted", tokenId);
            } catch (Exception ex) {
                // Do NOT throw; we don't want to impact user flow.
                // Hourly cleanup will remove it later.
                log.warn("Failed to delete used token {} (will be cleaned up later): {}", tokenId, ex.getMessage());
            }
        }, runAt);
    }

    // ============ PRIVATE HELPER METHODS ============

    private void validateRegistrationInput(UserDto registrationRequest) {
        // Sanitize inputs
        registrationRequest.setName(inputSanitizer.sanitize(registrationRequest.getName().trim()));
        registrationRequest.setEmail(inputSanitizer.sanitize(registrationRequest.getEmail().trim().toLowerCase()));
        registrationRequest.setPhoneNumber(inputSanitizer.sanitize(registrationRequest.getPhoneNumber().trim()));

        // Validate email format
        if (!inputSanitizer.isValidEmail(registrationRequest.getEmail())) {
            throw new RuntimeException("Invalid email format");
        }

        // Check for malicious input
        if (inputSanitizer.isMalicious(registrationRequest.getName()) ||
                inputSanitizer.isMalicious(registrationRequest.getEmail()) ||
                inputSanitizer.isMalicious(registrationRequest.getPhoneNumber())) {
            throw new RuntimeException("Invalid input detected");
        }
    }

    private User createUserFromDto(UserDto dto) {
        UserRole role = determineUserRole(dto.getRole());

        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneNumber(dto.getPhoneNumber())
                .companyName(dto.getCompanyName())
                .businessRegistrationNumber(dto.getBusinessRegistrationNumber())
                .userRole(role)
                .build();
    }

    private UserRole determineUserRole(String role) {
        if (role == null) return UserRole.ROLE_USER;

        return switch (role.toLowerCase()) {
            case "role_admin" -> UserRole.ROLE_ADMIN;
            case "role_customer_support" -> UserRole.ROLE_CUSTOMER_SUPPORT;
            case "role_company" -> UserRole.ROLE_COMPANY;
            default -> UserRole.ROLE_USER;
        };
    }

    private void performSecurityChecks(LoginRequest loginRequest, String clientIP) {
        // Rate limiting
        if (rateLimitService.isLoginLimited(loginRequest.getEmail())) {
            securityService.logSecurityEvent("RATE_LIMIT_LOGIN",
                    "Too many login attempts", clientIP, loginRequest.getEmail());
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }

        // Sanitize inputs
        loginRequest.setEmail(inputSanitizer.sanitize(loginRequest.getEmail().trim().toLowerCase()));

        // Check for malicious input
        if (inputSanitizer.isMalicious(loginRequest.getEmail()) ||
                inputSanitizer.isMalicious(loginRequest.getPassword())) {
            securityService.logSecurityEvent("MALICIOUS_LOGIN",
                    "Malicious login input", clientIP, loginRequest.getEmail());
            throw new RuntimeException("Invalid credentials");
        }

        // Check if IP is blocked
        if (securityService.isIpBlocked(clientIP)) {
            securityService.logSecurityEvent("BLOCKED_IP_LOGIN",
                    "Blocked IP login attempt", clientIP, loginRequest.getEmail());
            throw new RuntimeException("Access denied");
        }

        // Record attempt
        rateLimitService.recordAttempt(loginRequest.getEmail(), "LOGIN");
    }

    private User authenticateUser(LoginRequest loginRequest, String clientIP) {
        User user = userRepo.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    securityService.logSecurityEvent("LOGIN_FAILED",
                            "Email not found", clientIP, loginRequest.getEmail());
                    return new NotFoundException("Invalid email or password");
                });

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            securityService.logSecurityEvent("LOGIN_FAILED",
                    "Wrong password", clientIP, loginRequest.getEmail());
            throw new InvalidCredentialException("Invalid email or password");
        }

        return user;
    }

    private void handleLoginError(Exception e, String clientIP, String email) {
        if (!(e.getMessage().contains("verification") || e.getMessage().contains("code"))) {
            securityService.logSecurityEvent("LOGIN_ERROR",
                    "Login error: " + e.getMessage(), clientIP, email);
        }
        log.error("Login failed for email: {} - Error: {}", email, e.getMessage());
    }

    private void sendLoginNotification(User user, String clientIP, HttpServletRequest request) {
        try {
            String deviceInfo = deviceAuthService.getDeviceInfo(request);
            String location = securityService.getSimpleLocation(clientIP);
            deviceAuthService.sendLoginNotification(user, deviceInfo, location, request);
        } catch (Exception e) {
            log.warn("Failed to send login notification: {}", e.getMessage());
            // Don't fail login if notification fails
        }
    }

    private PasswordResetToken createPasswordResetToken(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRY_MINUTES));
        token.setUsed(false);
        return token;
    }

    private void validateResetToken(PasswordResetToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepo.delete(token);
            throw new TokenExpiredException("Reset token has expired");
        }
        if (token.isUsed()) {
            throw new InvalidCredentialException("This reset link has already been used");
        }
    }

    private void markTokenAsUsed(PasswordResetToken token) {
        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepo.save(token);
    }

    private void sendWelcomeEmail(User user) {
        emailService.sendEmail(
                user.getEmail(),
                "Welcome to CuttyPaws!",
                "Dear " + user.getName() + ",\n\nThank you for registering with CuttyPaws!"
        );
    }

    private void sendPasswordResetEmail(User user, String token) {
        String resetLink = "https://www.kinjomarket.com/reset-password?token=" + token;
        String body = String.format(
                "Dear %s,\n\n" +
                        "You requested a password reset. Click the link below (valid for %d minutes):\n%s\n\n" +
                        "If you didn't request this, please ignore this email.\n\n" +
                        "Best regards,\nCuttyPaws Team",
                user.getName(), PASSWORD_RESET_EXPIRY_MINUTES, resetLink
        );

        emailService.sendEmail(user.getEmail(), "Password Reset Request", body);
    }

    private void sendPasswordResetConfirmationEmail(User user) {
        emailService.sendEmail(
                user.getEmail(),
                "Password Reset Successful",
                "Your password has been successfully reset. If you didn't make this change, please contact support immediately."
        );
    }
}
