package com.cuttypaws.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiHelpIntentDto {
    private String helpType; // GENERAL, PET_HEALTH, PRODUCT_HELP, SERVICE_HELP, ORDER_HELP, PAYMENT_HELP, BOTH
    private String petType;
    private String serviceType;
    private String city;
    private String state;
    private String urgency;
    private List<String> symptoms;
    private List<String> keywords;
    private List<String> productKeywords;
    private String orderReference;
    private String paymentReference;
    private boolean emergency;
}