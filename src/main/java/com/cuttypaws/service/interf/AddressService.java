package com.cuttypaws.service.interf;


import com.cuttypaws.dto.AddressDto;
import com.cuttypaws.response.AddressResponse;

public interface AddressService {

    AddressResponse saveOrUpdateAddress(AddressDto addressDto);
}

