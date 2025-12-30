package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;

import java.util.List;

public interface WishlistService {
    WishlistDto addToWishlist(Long userId, Long productId);
    List<WishlistDto> getWishlistByUserId(Long userId);
    void removeFromWishlist(Long userId, Long productId);
}
