package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;

import com.cuttypaws.entity.User;
import com.cuttypaws.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

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

    UserDto getUserByIdWithAddress(Long id); // Fetch user with address by ID

    List<UserDto> getAllUsersWithAddress();

    UserResponse getUserInfoAndOrdersHistory();

    // Fetch all users with their addresses and orders
    List<UserDto> getAllUsersWithAddressAndOrders();

    UserResponse getAllUsersWithRoleCompany();

    UserResponse getCompanyWithProducts(Long companyId);
    //Response loginCompany(LoginRequest loginRequest);


}
