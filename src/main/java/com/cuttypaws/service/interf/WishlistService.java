package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;

import java.util.List;
import java.util.UUID;

public interface WishlistService {
    WishlistDto addToWishlist(UUID userId, Long productId);
    List<WishlistDto> getWishlistByUserId(UUID userId);
    void removeFromWishlist(UUID userId, Long productId);
}
