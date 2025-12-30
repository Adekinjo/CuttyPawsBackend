package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;

import com.cuttypaws.entity.*;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.AddressResponse;
import com.cuttypaws.service.interf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepo addressRepo;
    private final UserService userService;

    @Override
    public AddressResponse saveOrUpdateAddress(AddressDto addressDto) {

        User user = userService.getLoginUser();
        Address address = user.getAddress();

        if(address == null){
            address = new Address();
            address.setUser(user);
        }
        if(addressDto.getStreet() != null) address.setStreet(addressDto.getStreet());
        if(addressDto.getCity() != null) address.setCity(addressDto.getCity());
        if(addressDto.getState() != null) address.setState(addressDto.getState());
        if(addressDto.getZipcode() != null) address.setZipcode(addressDto.getZipcode());
        if(addressDto.getCountry() != null) address.setCountry(addressDto.getCountry());

        addressRepo.save(address);

        String message = (user.getAddress() == null) ? "Address successfully created" : "Address successfully updated";
        return AddressResponse.builder()
                .status(200)
                .timestamp(LocalDateTime.now())
                .message(message)
                .build();
    };



};
