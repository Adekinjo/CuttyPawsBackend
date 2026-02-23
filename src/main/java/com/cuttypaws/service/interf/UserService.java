package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;

import com.cuttypaws.entity.User;
import com.cuttypaws.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse registerUser(UserDto registrationRequest);

    UserResponse loginUser(LoginRequest loginRequest, HttpServletRequest request);

    UserResponse getAllUser();

    User getLoginUser();

    UserResponse getUserInfoAndOrderHistory();

    // ADD THIS NEW METHOD FOR TOKEN REFRESH
    UserResponse refreshToken(String refreshToken);

    UserResponse requestPasswordReset(String email);

    UserResponse resetPassword(String token, String newPassword);

    UserResponse updateUserProfile(UserDto userDto);

    UserResponse updateProfileImage(MultipartFile file);

    UserResponse updateCoverImage(MultipartFile file);

    UserDto getUserByIdWithAddress(UUID id); // Fetch user with address by ID

    List<UserDto> getAllUsersWithAddress();

    UserResponse getUserInfoAndOrdersHistory();

    // Fetch all users with their addresses and orders
    List<UserDto> getAllUsersWithAddressAndOrders();

    UserResponse getAllUsersWithRoleCompany();

    UserResponse getCompanyWithProducts(UUID companyId);
    //Response loginCompany(LoginRequest loginRequest);


}
