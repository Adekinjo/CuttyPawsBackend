package com.cuttypaws.service.impl;

import com.cuttypaws.dto.WishlistDto;
import com.cuttypaws.entity.*;
import com.cuttypaws.mapper.WishlistMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.service.interf.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistServiceImpl implements WishlistService {
    @Autowired
    private WishlistRepo wishlistRepository;

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private ProductRepo productRepository;

    @Autowired
    private WishlistMapper wishlistMapper;

    @Override
    public WishlistDto addToWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);
        wishlistRepository.save(wishlist);

        return wishlistMapper.wishlistDto(wishlist);
    }

    @Override
    public List<WishlistDto> getWishlistByUserId(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserId(userId);
        return wishlists.stream().map(wishlistMapper::wishlistDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }
}
