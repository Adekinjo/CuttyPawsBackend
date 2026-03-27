package com.cuttypaws.mapper;

import com.cuttypaws.dto.AddressDto;
import com.cuttypaws.dto.OrderItemDto;
import com.cuttypaws.dto.UserDto;
import com.cuttypaws.entity.Address;
import com.cuttypaws.entity.OrderItem;
import com.cuttypaws.entity.Product;
import com.cuttypaws.entity.ServiceProfile;
import com.cuttypaws.entity.User;
import com.cuttypaws.enums.ServiceStatus;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto mapUserToDtoBasic(User user){
        UserDto userDto = new UserDto();
        userDto.setId(user.getId().toString());
        userDto.setPhoneNumber(user.getPhoneNumber());
        userDto.setEmail(user.getEmail());
        userDto.setCompanyName(user.getCompanyName());
        userDto.setBusinessRegistrationNumber(user.getBusinessRegistrationNumber());
        userDto.setProfileImageUrl(user.getProfileImageUrl());
        userDto.setCoverImageUrl(user.getCoverImageUrl());
        userDto.setRegDate(user.getCreatedAt());
        userDto.setRole(user.getUserRole() != null ? user.getUserRole().name() : null);
        userDto.setName(user.getName());
        userDto.setIsBlocked(user.getIsBlocked());
        userDto.setBlockedReason(user.getBlockedReason());
        userDto.setBlockedAt(user.getBlockedAt());
        userDto.setIsActive(user.isActive());
        userDto.setIsServiceProvider(user.getIsServiceProvider());
        userDto.setPetsCount(user.getPetsCount());

        if (Boolean.TRUE.equals(user.getIsServiceProvider())
                && user.getServiceProfile() != null
                && user.getServiceProfile().getStatus() == ServiceStatus.ACTIVE) {
            userDto.setDisplayLabel(buildServiceDisplayLabel(user.getServiceProfile()));
        } else {
            int petsCount = user.getPetsCount() != null ? user.getPetsCount() : 0;
            userDto.setDisplayLabel(buildPetOwnerDisplayLabel(petsCount));
        }

        return userDto;
    }

    private String buildPetOwnerDisplayLabel(int petsCount) {
        if (petsCount <= 0) {
            return "Pet owner";
        }
        if (petsCount == 1) {
            return "Owns 1 pet";
        }
        return "Owns " + petsCount + " pets";
    }

    private String buildServiceDisplayLabel(ServiceProfile serviceProfile) {
        String serviceName = switch (serviceProfile.getServiceType()) {
            case PET_WALKER -> "Pet Walker";
            case VETERINARIAN -> "Veterinarian";
            case PET_HOSPITAL -> "Pet Hospital";
            case PET_DAYCARE -> "Pet Daycare";
            case PET_TRAINER -> "Pet Trainer";
            case PET_SELLER -> "Pet Seller";
            case ADOPTION_CENTER -> "Adoption Center";
            case GROOMER -> "Groomer";
            case PET_BOARDING -> "Pet Boarding";
            case PET_SITTER -> "Pet Sitter";
            case BREEDER -> "Breeder";
            case RESCUE_SHELTER -> "Rescue Shelter";
        };

        String city = serviceProfile.getCity();
        String state = serviceProfile.getState();

        if (city != null && !city.isBlank() && state != null && !state.isBlank()) {
            return serviceName + " • " + city + ", " + state;
        }

        if (city != null && !city.isBlank()) {
            return serviceName + " • " + city;
        }

        return serviceName;
    }

    public UserDto mapUserToDtoPlusAddress(User user){
        UserDto userDto = mapUserToDtoBasic(user);
        if (user.getAddress() != null){
            AddressDto addressDto = mapAddressToDtoBasic(user.getAddress());
            userDto.setAddress(addressDto);
        }
        return userDto;
    }

    public UserDto mapUserToDtoPlusAddressAndOrderHistory(User user) {
        UserDto userDto = mapUserToDtoPlusAddress(user);

        if (user.getOrderItemsList() != null && !user.getOrderItemsList().isEmpty()) {
            userDto.setOrderItemList(user.getOrderItemsList()
                    .stream()
                    .map(this::mapOrderItemToDtoPlusProduct)
                    .collect(Collectors.toList()));
        }
        return userDto;
    }

    public OrderItemDto mapOrderItemToDtoPlusProduct(OrderItem orderItem) {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(orderItem.getId());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        dto.setStatus(orderItem.getOrderStatus().name());
        dto.setCreatedAt(orderItem.getCreatedAt());
        dto.setSelectedSize(orderItem.getSize());
        dto.setSelectedColor(orderItem.getColor());

        if (orderItem.getProduct() != null) {
            Product product = orderItem.getProduct();
            dto.setProductName(product.getName());
            dto.setPrice(product.getNewPrice());

            if (!product.getImages().isEmpty()) {
                dto.setProductImageUrl(product.getImages().get(0).getImageUrl());
            }
        }

        return dto;
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
}