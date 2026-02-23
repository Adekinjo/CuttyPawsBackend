package com.cuttypaws.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder  //  new
public class UserDto {


    private String id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    private String password;

    private String profileImageUrl;
    private String coverImageUrl;

    private LocalDateTime regDate;

    private String role;
    private List<OrderItemDto> orderItemList;
    private AddressDto address;
    private List<OrderDto> orders;

    private String companyName;
    private String businessRegistrationNumber;

    private Boolean isBlocked;
    private String blockedReason;
    private LocalDateTime blockedAt;
    private Boolean isActive;
}

