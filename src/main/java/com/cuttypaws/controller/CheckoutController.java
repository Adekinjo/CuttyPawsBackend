package com.cuttypaws.controller;

import com.cuttypaws.dto.CreateCheckoutSessionRequest;
import com.cuttypaws.response.CheckoutSessionResponse;
import com.cuttypaws.service.interf.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/session")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SELLER', 'SERVICE_PROVIDER')")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @RequestBody CreateCheckoutSessionRequest request
    ) {
        return ResponseEntity.ok(checkoutService.createCheckoutSession(request));
    }
}