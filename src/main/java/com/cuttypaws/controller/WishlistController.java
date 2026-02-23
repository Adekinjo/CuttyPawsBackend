package com.cuttypaws.controller;

import com.cuttypaws.dto.WishlistDto;
import com.cuttypaws.dto.WishlistRequest;
import com.cuttypaws.service.interf.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/wishlist")
public class WishlistController {
    @Autowired
    private WishlistService wishlistService;

    @PostMapping("/add")
    public ResponseEntity<WishlistDto> addToWishlist(@RequestBody WishlistRequest request) {
        WishlistDto wishlistDTO = wishlistService.addToWishlist(request.getUserId(), request.getProductId());
        return ResponseEntity.ok(wishlistDTO);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WishlistDto>> getWishlistByUserId(@PathVariable UUID userId) {
        List<WishlistDto> wishlist = wishlistService.getWishlistByUserId(userId);
        return ResponseEntity.ok(wishlist);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeFromWishlist(@RequestParam UUID userId, @RequestParam Long productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.noContent().build();
    }
}
