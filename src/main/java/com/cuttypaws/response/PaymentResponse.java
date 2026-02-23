package com.cuttypaws.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponse {

    private int status;
    private String message;
    private String authorizationUrl;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private Long paymentId;
    private UUID userId;
}
