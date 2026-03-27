package com.cuttypaws.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceProviderRegistrationRequest {

    @Valid
    @NotNull
    private UserDto user;

    @Valid
    @NotNull
    private ServiceProfileRequestDto serviceProfile;
}