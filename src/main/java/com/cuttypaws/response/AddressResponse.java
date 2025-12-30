package com.cuttypaws.response;

import com.cuttypaws.dto.AddressDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressResponse {

    private int status;
    private String message;
    private LocalDateTime timestamp;
    private AddressDto address;
}
