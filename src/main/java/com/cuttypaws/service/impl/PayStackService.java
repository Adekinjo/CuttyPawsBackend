package com.cuttypaws.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PayStackService {

    @Value("${paystack.secret.key}")
    private String payStackSecretKey;

    @Value("${app.base.url:https://kinjomarket.com}")
    private String baseUrl;

    private static final String INITIALIZE_PAYMENT_URL = "https://api.paystack.co/transaction/initialize";
    private static final String VERIFY_PAYMENT_URL = "https://api.paystack.co/transaction/verify/";

    public Map<String, String> initializePayment(BigDecimal amount, String email, String currency) throws Exception {
        log.info("Initializing Paystack payment for email: {}, amount: {}", email, amount);

        // Validate Paystack key
        if (payStackSecretKey == null || payStackSecretKey.trim().isEmpty()) {
            throw new RuntimeException("Paystack secret key is not configured");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(INITIALIZE_PAYMENT_URL);
            request.setHeader("Authorization", "Bearer " + payStackSecretKey);
            request.setHeader("Content-Type", "application/json");

            // Convert amount to kobo
            int amountInKobo = amount.multiply(BigDecimal.valueOf(100)).intValueExact();

            JSONObject json = new JSONObject();
            json.put("amount", amountInKobo);
            json.put("email", email);
            json.put("currency", currency);

            // Add callback URL for production
            String callbackUrl = baseUrl + "/payment-callback";
            json.put("callback_url", callbackUrl);
            log.info("Using callback URL: {}", callbackUrl);

            log.info("Paystack request payload: {}", json.toString());

            StringEntity entity = new StringEntity(json.toString());
            request.setEntity(entity);

            String response = EntityUtils.toString(httpClient.execute(request).getEntity());
            JSONObject responseJson = new JSONObject(response);

            log.info("Paystack API response: {}", responseJson.toString());

            if (!responseJson.getBoolean("status")) {
                String errorMessage = responseJson.getString("message");
                log.error("Paystack API error: {}", errorMessage);
                throw new RuntimeException("Paystack API error: " + errorMessage);
            }

            JSONObject data = responseJson.getJSONObject("data");
            String authorizationUrl = data.getString("authorization_url");
            String reference = data.getString("reference");

            if (authorizationUrl == null || authorizationUrl.isEmpty()) {
                throw new RuntimeException("Paystack returned empty authorization URL");
            }

            Map<String, String> result = new HashMap<>();
            result.put("authorizationUrl", authorizationUrl);
            result.put("reference", reference);

            log.info("Payment initialized successfully. Reference: {}", reference);
            return result;

        } catch (Exception e) {
            log.error("Error calling Paystack API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize payment with Paystack: " + e.getMessage());
        }
    }

    public boolean verifyPayment(String reference) throws Exception {
        log.info("Verifying Paystack payment with reference: {}", reference);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(VERIFY_PAYMENT_URL + reference);
            request.setHeader("Authorization", "Bearer " + payStackSecretKey);
            request.setHeader("Content-Type", "application/json");

            log.info("Sending verification request to Paystack for reference: {}", reference);

            String response = EntityUtils.toString(httpClient.execute(request).getEntity());
            JSONObject responseJson = new JSONObject(response);

            log.info("Paystack verification response: {}", responseJson.toString());

            if (!responseJson.getBoolean("status")) {
                String errorMessage = responseJson.getString("message");
                log.error("Paystack verification failed: {}", errorMessage);
                throw new RuntimeException("Paystack verification error: " + errorMessage);
            }

            JSONObject data = responseJson.getJSONObject("data");
            String status = data.getString("status");
            String gatewayResponse = data.optString("gateway_response", "N/A");

            log.info("Payment verification - Status: {}, Gateway Response: {}", status, gatewayResponse);

            boolean isSuccessful = "success".equals(status);

            if (isSuccessful) {
                log.info("✅ Payment verification SUCCESSFUL for reference: {}", reference);
            } else {
                log.warn("❌ Payment verification FAILED for reference: {}. Status: {}", reference, status);
            }

            return isSuccessful;
        } catch (Exception e) {
            log.error("Error verifying Paystack payment for reference {}: {}", reference, e.getMessage(), e);
            throw new RuntimeException("Failed to verify payment with Paystack: " + e.getMessage());
        }
    }
}

