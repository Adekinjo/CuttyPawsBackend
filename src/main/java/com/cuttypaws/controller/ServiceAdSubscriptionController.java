package com.cuttypaws.controller;

import com.cuttypaws.dto.ConfirmServiceAdPaymentRequest;
import com.cuttypaws.dto.CreateServiceAdSubscriptionRequest;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.interf.ServiceAdSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/service-ads")
@RequiredArgsConstructor
public class ServiceAdSubscriptionController {

    private final ServiceAdSubscriptionService serviceAdSubscriptionService;

    @PostMapping("/my-subscriptions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> createMyAdSubscription(
            @Valid @RequestBody CreateServiceAdSubscriptionRequest request
    ) {
        UserResponse response = serviceAdSubscriptionService.createMyAdSubscription(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/my-subscriptions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyAdSubscriptions() {
        UserResponse response = serviceAdSubscriptionService.getMyAdSubscriptions();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/my-subscriptions/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyActiveAdSubscription() {
        UserResponse response = serviceAdSubscriptionService.getMyActiveAdSubscription();
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}