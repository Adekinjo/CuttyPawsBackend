package com.cuttypaws.mapper;

import com.cuttypaws.dto.AddressDto;
import com.cuttypaws.dto.OrderItemDto;
import com.cuttypaws.dto.UserDto;
import com.cuttypaws.entity.Address;
import com.cuttypaws.entity.OrderItem;
import com.cuttypaws.entity.Product;
import com.cuttypaws.entity.User;
import org.springframework.stereotype.Component;


@Component
public class OrderMapper {



    //orderItem to DTO plus product
    public OrderItemDto mapOrderItemToDtoPlusProduct(OrderItem orderItem) {
        OrderItemDto dto = new OrderItemDto();
        // Map basic fields
        dto.setId(orderItem.getId());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        dto.setStatus(orderItem.getOrderStatus().name());
        dto.setCreatedAt(orderItem.getCreatedAt());

        // Map selected variants
        dto.setSelectedSize(orderItem.getSize());
        dto.setSelectedColor(orderItem.getColor());

        // Map essential product info
        if (orderItem.getProduct() != null) {
            Product product = orderItem.getProduct();
            dto.setProductName(product.getName());
            dto.setPrice(product.getNewPrice());

            // Get first image URL
            if (!product.getImages().isEmpty()) {
                dto.setProductImageUrl(product.getImages().get(0).getImageUrl());
            }
        }

        return dto;
    }
    //OrderItem to DTO plus product and user
    public OrderItemDto mapOrderItemToDtoPlusProductAndUser(OrderItem orderItem){
        OrderItemDto orderItemDto = mapOrderItemToDtoPlusProduct(orderItem);

        if (orderItem.getUser() != null){
            UserDto userDto = mapUserToDtoPlusAddress(orderItem.getUser());
            orderItemDto.setUser(userDto);
        }
        return orderItemDto;
    }


    public UserDto mapUserToDtoBasic(User user){
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
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

    //Address to DTO Basic
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
