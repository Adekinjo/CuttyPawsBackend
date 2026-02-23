package com.cuttypaws.controller;

import com.cuttypaws.dto.UserDto;
import com.cuttypaws.exception.InvalidCredentialException;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.interf.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/get-all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse>  getAllUsers(){
        return ResponseEntity.ok(userService.getAllUser());
    }
    @GetMapping("/my-info")
    public ResponseEntity<UserResponse> getUserInfoAndOrderHistory() {
        return ResponseEntity.ok(userService.getUserInfoAndOrdersHistory());
    }

    // to update a user
    @PutMapping("/update")
    public ResponseEntity<UserResponse> updateUserProfile(@RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.updateUserProfile(userDto));
    }

    @PutMapping(value = "/update-profile-image", consumes = "multipart/form-data")
    public ResponseEntity<UserResponse> updateProfileImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateProfileImage(file));
    }

    @PutMapping(value = "/update-cover-image", consumes = "multipart/form-data")
    public ResponseEntity<UserResponse> updateCoverImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateCoverImage(file));
    }

    // to get a user by id
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        UserDto userDto = userService.getUserByIdWithAddress(id);
        return userDto != null ? ResponseEntity.ok(userDto) : ResponseEntity.notFound().build();
    }


    // to get all users only
    @GetMapping("/get-alls")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUser() {
        List<UserDto> users = userService.getAllUsersWithAddress();
        return ResponseEntity.ok(users);
    }

    // to get all user information
    @GetMapping("/get-all-info")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsersInfo() {
        List<UserDto> users = userService.getAllUsersWithAddressAndOrders();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/role-company")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Only admins can access this endpoint
    public ResponseEntity<UserResponse> getAllUsersWithRoleCompany() {
        try {
            UserResponse response = userService.getAllUsersWithRoleCompany();
            return ResponseEntity.ok(response);
        } catch (InvalidCredentialException e) {
            return ResponseEntity.status(403).body(
                    UserResponse.builder()
                            .status(403)
                            .message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    UserResponse.builder()
                            .status(500)
                            .message("Failed to fetch users with ROLE_COMPANY: " + e.getMessage())
                            .build()
            );
        }
    }

}
