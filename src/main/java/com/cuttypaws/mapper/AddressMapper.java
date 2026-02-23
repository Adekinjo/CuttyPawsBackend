package com.cuttypaws.mapper;

import com.cuttypaws.dto.AddressDto;
import com.cuttypaws.dto.UserDto;
import com.cuttypaws.entity.Address;
import com.cuttypaws.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {



    public UserDto mapUserToDtoBasic(User user){
        UserDto userDto = new UserDto();
        userDto.setId(user.getId().toString());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setEmail(user.getEmail());
        if (user.getCompanyName() != null) {
            userDto.setCompanyName(user.getCompanyName());
        }
        userDto.setRegDate(user.getCreatedAt());
        userDto.setRole(user.getUserRole().name());
        userDto.setName(user.getName());
        return userDto;

    }

    public AddressDto mapAddressToDtoBasic(Address address){
        AddressDto addressDto = new AddressDto();
        addressDto.setId(address.getId());
        addressDto.setCity(address.getCity());
        addressDto.setStreet(address.getStreet());
        addressDto.setState(address.getState());
        addressDto.setCountry(address.getCountry());
        addressDto.setZipcode(address.getZipcode());
        return addressDto;
    }


    public UserDto mapUserToDtoPlusAddress(User user){
        UserDto userDto = mapUserToDtoBasic(user);
        if (user.getAddress() != null){
            AddressDto addressDto = mapAddressToDtoBasic(user.getAddress());
            userDto.setAddress(addressDto);
        }
        return userDto;
    }
}

