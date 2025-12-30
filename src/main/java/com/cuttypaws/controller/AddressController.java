package com.cuttypaws.controller;

import com.cuttypaws.dto.AddressDto;
import com.cuttypaws.response.AddressResponse;
import com.cuttypaws.service.interf.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping("/save")
    public ResponseEntity<AddressResponse> saveAndUpdateAddress(@RequestBody AddressDto addressDto){
        return ResponseEntity.ok(addressService.saveOrUpdateAddress(addressDto));
    }
}
