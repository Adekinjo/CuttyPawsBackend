package com.cuttypaws.mapper;

import com.cuttypaws.dto.WishlistDto;
import com.cuttypaws.entity.Wishlist;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {


    public WishlistDto wishlistDto(Wishlist wishlist) {
        WishlistDto dto = new WishlistDto();
        dto.setId(wishlist.getId());
        dto.setUserId(wishlist.getUser().getId());
        dto.setProductId(wishlist.getProduct().getId());
        dto.setProductName(wishlist.getProduct().getName());
        dto.setPrice(wishlist.getProduct().getNewPrice());
        if (wishlist.getProduct().getImages() != null
                && !wishlist.getProduct().getImages().isEmpty()) {
            dto.setProductImage(wishlist.getProduct().getImages().get(0).getImageUrl());
        } else {
            dto.setProductImage("default-image-url.jpg"); // Fallback URL
        }
        return dto;
    }
}
