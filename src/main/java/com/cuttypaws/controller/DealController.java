// DealController.java
package com.cuttypaws.controller;

import com.cuttypaws.dto.DealDto;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.response.DealResponse;
import com.cuttypaws.service.interf.DealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DealResponse> createDeal(@RequestBody DealDto dealDto) {
        try {
            return ResponseEntity.ok(dealService.createDeal(dealDto.getProduct().getId(), dealDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(DealResponse.builder()
                            .status(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PutMapping("/update/{dealId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DealResponse> updateDeal(@PathVariable Long dealId, @RequestBody DealDto dealDto) {
        try {
            return ResponseEntity.ok(dealService.updateDeal(dealId, dealDto));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(DealResponse.builder()
                            .status(404)
                            .message(e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/delete/{dealId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DealResponse> deleteDeal(@PathVariable Long dealId) {
        return ResponseEntity.ok(dealService.deleteDeal(dealId));
    }

    @GetMapping("/all/active")
    public ResponseEntity<DealResponse> getActiveDeals() {
        return ResponseEntity.ok(dealService.getAllActiveDeals());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<DealResponse> getDealByProduct(@PathVariable Long productId) {
        try {
            return ResponseEntity.ok(dealService.getDealByProductId(productId));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(DealResponse.builder()
                            .status(404)
                            .message(e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/{dealId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DealResponse> toggleDealStatus(
            @PathVariable Long dealId,
            @RequestParam boolean active) {
        return ResponseEntity.ok(dealService.toggleDealStatus(dealId, active));
    }
}