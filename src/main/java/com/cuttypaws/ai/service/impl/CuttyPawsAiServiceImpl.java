package com.cuttypaws.ai.service.impl;

import com.cuttypaws.ai.config.AiConfig.AiModelCatalog;
import com.cuttypaws.ai.config.CuttyPawsAiProperties;
import com.cuttypaws.ai.dto.*;
import com.cuttypaws.ai.prompt.AiSystemPrompts;
import com.cuttypaws.ai.service.interf.AiOrderSupportService;
import com.cuttypaws.ai.service.interf.AiRecommendationService;
import com.cuttypaws.ai.service.interf.CuttyPawsAiService;
import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.dto.ServiceProfileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuttyPawsAiServiceImpl implements CuttyPawsAiService {

    private final ChatClient chatClient;
    private final AiModelCatalog modelCatalog;
    private final CuttyPawsAiProperties aiProperties;
    private final AiRecommendationService aiRecommendationService;
    private final AiOrderSupportService aiOrderSupportService;

    @Override
    public AiChatResponse aiHelp(AiChatRequest request) {
        return buildAiSupportResponse(request, null);
    }

    @Override
    public AiChatResponse aiHelpWithImage(UUID userId, String prompt, MultipartFile image) {
        AiChatRequest request = AiChatRequest.builder()
                .userId(userId)
                .prompt(prompt)
                .feature("AI_SUPPORT")
                .build();

        return buildAiSupportResponse(request, image);
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        try {
            String answer = chatClient.prompt()
                    .system(AiSystemPrompts.GENERAL_ASSISTANT)
                    .user("""
                        Feature: %s
                        User prompt: %s
                        """.formatted(
                            request.getFeature() != null ? request.getFeature() : "GENERAL",
                            request.getPrompt()
                    ))
                    .call()
                    .content();

            return AiChatResponse.builder()
                    .success(true)
                    .answer(answer)
                    .model(modelCatalog.defaultChatModel())
                    .feature(request.getFeature() != null ? request.getFeature() : "GENERAL")
                    .build();

        } catch (Exception e) {
            log.error("AI chat failed", e);

            String message = "Unable to process your request right now.";

            if (e.getMessage() != null && e.getMessage().contains("insufficient_quota")) {
                message = "OpenAI quota exceeded. Check billing, usage limits, and project quota.";
            }

            return AiChatResponse.builder()
                    .success(false)
                    .answer(message)
                    .model(modelCatalog.defaultChatModel())
                    .feature(request.getFeature() != null ? request.getFeature() : "GENERAL")
                    .build();
        }
    }

    @Override
    public AiChatResponse petHealthAdvice(PetHealthAssistantRequest request) {
        try {
            String answer = chatClient.prompt()
                    .system(AiSystemPrompts.PET_HEALTH)
                    .user("""
                            Pet owner question:
                            %s
                            """.formatted(request.getQuestion()))
                    .call()
                    .content();

            return AiChatResponse.builder()
                    .success(true)
                    .answer(answer)
                    .model(modelCatalog.advancedReasoningModel())
                    .feature("PET_HEALTH_TEXT")
                    .disclaimer(aiProperties.getHealth().isDisclaimerEnabled()
                            ? "AI guidance only. This is not a veterinary diagnosis."
                            : null)
                    .build();

        } catch (Exception e) {
            log.error("Pet health text advice failed", e);
            return AiChatResponse.builder()
                    .success(false)
                    .answer("Unable to analyze the pet health question right now.")
                    .model(modelCatalog.advancedReasoningModel())
                    .feature("PET_HEALTH_TEXT")
                    .disclaimer(aiProperties.getHealth().isDisclaimerEnabled()
                            ? "AI guidance only. This is not a veterinary diagnosis."
                            : null)
                    .build();
        }
    }

    @Override
    public AiChatResponse petHealthImageAdvice(String question, MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return AiChatResponse.builder()
                        .success(false)
                        .answer("Image file is required.")
                        .model(modelCatalog.advancedReasoningModel())
                        .feature("PET_HEALTH_IMAGE")
                        .build();
            }

            ByteArrayResource imageResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            };

            MimeType mimeType = (image.getContentType() != null && !image.getContentType().isBlank())
                    ? MimeTypeUtils.parseMimeType(image.getContentType())
                    : MimeTypeUtils.IMAGE_JPEG;

            String answer = chatClient.prompt()
                    .system(AiSystemPrompts.PET_HEALTH_IMAGE)
                    .user(userSpec -> userSpec
                            .text("""
                                    Pet owner question:
                                    %s
                                    """.formatted(question))
                            .media(mimeType, imageResource)
                    )
                    .call()
                    .content();

            return AiChatResponse.builder()
                    .success(true)
                    .answer(answer)
                    .model(modelCatalog.advancedReasoningModel())
                    .feature("PET_HEALTH_IMAGE")
                    .disclaimer(aiProperties.getHealth().isDisclaimerEnabled()
                            ? "AI guidance only. This is not a veterinary diagnosis."
                            : null)
                    .build();

        } catch (Exception e) {
            log.error("Pet health image advice failed", e);
            return AiChatResponse.builder()
                    .success(false)
                    .answer("Unable to analyze the pet image right now.")
                    .model(modelCatalog.advancedReasoningModel())
                    .feature("PET_HEALTH_IMAGE")
                    .disclaimer(aiProperties.getHealth().isDisclaimerEnabled()
                            ? "AI guidance only. This is not a veterinary diagnosis."
                            : null)
                    .build();
        }
    }

    @Override
    public UnifiedSearchResultDto parseUnifiedSearch(UnifiedAiSearchRequest request) {
        try {
            int radius = request.getRadiusMiles() != null
                    ? request.getRadiusMiles()
                    : aiProperties.getSearch().getDefaultRadiusMiles();

            BeanOutputConverter<UnifiedSearchResultDto> converter =
                    new BeanOutputConverter<>(UnifiedSearchResultDto.class);

            String content = chatClient.prompt()
                    .system(AiSystemPrompts.SEARCH_ASSISTANT)
                    .user("""
                            User query: %s
                            Latitude: %s
                            Longitude: %s
                            Radius miles: %s

                            %s
                            """.formatted(
                            request.getQuery(),
                            request.getLatitude(),
                            request.getLongitude(),
                            radius,
                            converter.getFormat()
                    ))
                    .call()
                    .content();

            return converter.convert(content);

        } catch (Exception e) {
            log.error("Unified search parse failed", e);

            UnifiedSearchResultDto fallback = new UnifiedSearchResultDto();
            fallback.setRadiusMiles(
                    request.getRadiusMiles() != null
                            ? request.getRadiusMiles()
                            : aiProperties.getSearch().getDefaultRadiusMiles()
            );
            return fallback;
        }
    }

    private AiChatResponse buildAiSupportResponse(AiChatRequest request, MultipartFile image) {
        try {
            AiHelpIntentDto intent = parseHelpIntent(request.getPrompt());

            String city = firstNonBlank(intent.getCity(), request.getCity());
            String state = firstNonBlank(intent.getState(), request.getState());
            String productSearchText = buildProductSearchText(intent);

            if ((productSearchText == null || productSearchText.isBlank()) && image != null && !image.isEmpty()) {
                AiImageProductIntentDto imageIntent = extractImageProductIntent(request.getPrompt(), image);
                if (imageIntent != null
                        && imageIntent.getProductQuery() != null
                        && !imageIntent.getProductQuery().isBlank()) {
                    productSearchText = imageIntent.getProductQuery();
                }
            }

            boolean hasImageProductIntent = productSearchText != null && !productSearchText.isBlank();

            List<ProductDto> products =
                    (shouldRecommendProducts(intent) || (image != null && !image.isEmpty() && hasImageProductIntent))
                            ? aiRecommendationService.recommendProducts(productSearchText)
                            : List.of();

            List<ServiceProfileDto> services = shouldRecommendServices(intent)
                    ? aiRecommendationService.recommendServices(intent.getServiceType(), city, state)
                    : List.of();

            List<String> externalSearchKeywords =
                    intent.getProductKeywords() != null && !intent.getProductKeywords().isEmpty()
                            ? intent.getProductKeywords()
                            : (productSearchText != null && !productSearchText.isBlank()
                            ? List.of(productSearchText)
                            : List.of());

            List<ExternalProductSuggestionDto> externalProducts =
                    products.isEmpty() && !externalSearchKeywords.isEmpty()
                            ? aiRecommendationService.recommendExternalProducts(externalSearchKeywords)
                            : List.of();

            AiSupportContextDto context = AiSupportContextDto.builder()
                    .products(products)
                    .services(services)
                    .orders(needsOrderContext(intent)
                            ? aiOrderSupportService.getRecentOrders(request.getUserId(), 5)
                            : List.of())
                    .payments(resolvePayments(request.getUserId(), intent))
                    .build();

            String answer = generateSupportAnswer(request.getPrompt(), intent, context, image);

            return AiChatResponse.builder()
                    .success(true)
                    .answer(answer)
                    .model(modelCatalog.advancedReasoningModel())
                    .feature("AI_SUPPORT")
                    .disclaimer(intent.isEmergency() || "PET_HEALTH".equalsIgnoreCase(intent.getHelpType())
                            ? "AI guidance only. This is not a veterinary diagnosis."
                            : null)
                    .recommendationType(resolveRecommendationType(products, services))
                    .recommendedProducts(products)
                    .recommendedServices(services)
                    .externalProductSuggestions(externalProducts)
                    .followUpQuestions(buildFollowUps(intent))
                    .build();

        } catch (Exception e) {
            log.error("AI support failed", e);
            return AiChatResponse.builder()
                    .success(false)
                    .answer("Unable to process your support request right now.")
                    .model(modelCatalog.advancedReasoningModel())
                    .feature("AI_SUPPORT")
                    .recommendationType("NONE")
                    .recommendedProducts(List.of())
                    .recommendedServices(List.of())
                    .externalProductSuggestions(List.of())
                    .followUpQuestions(List.of())
                    .build();
        }
    }

    private AiHelpIntentDto parseHelpIntent(String prompt) {
        BeanOutputConverter<AiHelpIntentDto> converter =
                new BeanOutputConverter<>(AiHelpIntentDto.class);

        String content = chatClient.prompt()
                .system(AiSystemPrompts.AI_HELP_ROUTER)
                .user("""
                    User message:
                    %s

                    %s
                    """.formatted(prompt, converter.getFormat()))
                .call()
                .content();

        AiHelpIntentDto result = converter.convert(content);
        return result != null ? result : new AiHelpIntentDto();
    }

    private String generateSupportAnswer(
            String prompt,
            AiHelpIntentDto intent,
            AiSupportContextDto context,
            MultipartFile image
    ) throws Exception {

        String contextText = """
            Help type: %s
            Emergency: %s
            Pet type: %s
            Service type: %s
            City: %s
            State: %s
            Matching products: %s
            Matching services: %s
            Recent orders: %s
            Payment context: %s
            """.formatted(
                intent.getHelpType(),
                intent.isEmergency(),
                intent.getPetType(),
                intent.getServiceType(),
                intent.getCity(),
                intent.getState(),
                summarizeProducts(context.getProducts()),
                summarizeServices(context.getServices()),
                summarizeOrders(context.getOrders()),
                summarizePayments(context.getPayments())
        );

        if (image == null || image.isEmpty()) {
            return chatClient.prompt()
                    .system(AiSystemPrompts.AI_SUPPORT_ASSISTANT)
                    .user("""
                        User message:
                        %s

                        Platform context:
                        %s
                        """.formatted(prompt, contextText))
                    .call()
                    .content();
        }

        ByteArrayResource imageResource = new ByteArrayResource(image.getBytes()) {
            @Override
            public String getFilename() {
                return image.getOriginalFilename();
            }
        };

        MimeType mimeType = image.getContentType() != null && !image.getContentType().isBlank()
                ? MimeTypeUtils.parseMimeType(image.getContentType())
                : MimeTypeUtils.IMAGE_JPEG;

        return chatClient.prompt()
                .system(AiSystemPrompts.AI_SUPPORT_ASSISTANT)
                .user(userSpec -> userSpec
                        .text("""
                            User message:
                            %s

                            Platform context:
                            %s
                            """.formatted(prompt, contextText))
                        .media(mimeType, imageResource))
                .call()
                .content();
    }

//    private String summarizeProducts(List<ProductDto> products) {
//        if (products == null || products.isEmpty()) {
//            return "None";
//        }
//
//        return products.stream()
//                .limit(5)
//                .map(product -> {
//                    String name = product.getName() != null ? product.getName() : "Unnamed Product";
//                    String price = product.getNewPrice() != null ? product.getNewPrice().toString() : "N/A";
//                    String category = product.getCategory() != null ? product.getCategory() : "Uncategorized";
//                    return "%s ($%s, %s)".formatted(name, price, category);
//                })
//                .collect(Collectors.joining(", "));
//    }
//
//    private String summarizeServices(List<ServiceProfileDto> services) {
//        if (services == null || services.isEmpty()) {
//            return "None";
//        }
//
//        return services.stream()
//                .limit(5)
//                .map(service -> {
//                    String businessName = service.getBusinessName() != null ? service.getBusinessName() : "Service Provider";
//                    String type = service.getServiceType() != null ? service.getServiceType().name() : "UNKNOWN";
//                    String city = service.getCity() != null ? service.getCity() : "";
//                    String state = service.getState() != null ? service.getState() : "";
//                    return "%s [%s, %s, %s]".formatted(businessName, type, city, state);
//                })
//                .collect(Collectors.joining(", "));
//    }
//
//    private String summarizeOrders(List<AiOrderSummaryDto> orders) {
//        if (orders == null || orders.isEmpty()) {
//            return "None";
//        }
//
//        return orders.stream()
//                .limit(5)
//                .map(order -> "order %s total %s".formatted(order.getOrderId(), order.getTotalPrice()))
//                .collect(Collectors.joining(", "));
//    }
//
//    private String summarizePayments(List<AiPaymentSummaryDto> payments) {
//        if (payments == null || payments.isEmpty()) {
//            return "None";
//        }
//
//        return payments.stream()
//                .limit(5)
//                .map(payment -> "payment %s %s %s".formatted(
//                        payment.getPaymentId(),
//                        payment.getAmount(),
//                        payment.getStatus()))
//                .collect(Collectors.joining(", "));
//    }
    private String summarizeProducts(List<ProductDto> products) {
        if (products == null || products.isEmpty()) {
            return "None";
        }

        return products.stream()
                .limit(3)
                .map(product -> {
                    String name = product.getName() != null ? product.getName() : "Unnamed Product";
                    String price = product.getNewPrice() != null ? product.getNewPrice().toString() : "N/A";
                    return "%s ($%s)".formatted(name, price);
                })
                .collect(Collectors.joining(", "));
    }
    private String summarizeServices(List<ServiceProfileDto> services) {
        if (services == null || services.isEmpty()) {
            return "None";
        }

        return services.stream()
                .limit(3)
                .map(service -> {
                    String businessName = service.getBusinessName() != null ? service.getBusinessName() : "Service Provider";
                    String type = service.getServiceType() != null ? service.getServiceType().name() : "UNKNOWN";
                    return "%s [%s]".formatted(businessName, type);
                })
                .collect(Collectors.joining(", "));
    }
    private String summarizeOrders(List<AiOrderSummaryDto> orders) {
        if (orders == null || orders.isEmpty()) {
            return "None";
        }

        return orders.stream()
                .limit(3)
                .map(order -> "order %s".formatted(order.getOrderId()))
                .collect(Collectors.joining(", "));
    }
    private String summarizePayments(List<AiPaymentSummaryDto> payments) {
        if (payments == null || payments.isEmpty()) {
            return "None";
        }

        return payments.stream()
                .limit(3)
                .map(payment -> "payment %s".formatted(payment.getPaymentId()))
                .collect(Collectors.joining(", "));
    }

    private AiImageProductIntentDto extractImageProductIntent(String prompt, MultipartFile image) throws Exception {
        BeanOutputConverter<AiImageProductIntentDto> converter =
                new BeanOutputConverter<>(AiImageProductIntentDto.class);

        ByteArrayResource imageResource = new ByteArrayResource(image.getBytes()) {
            @Override
            public String getFilename() {
                return image.getOriginalFilename();
            }
        };

        MimeType mimeType = image.getContentType() != null && !image.getContentType().isBlank()
                ? MimeTypeUtils.parseMimeType(image.getContentType())
                : MimeTypeUtils.IMAGE_JPEG;

        String content = chatClient.prompt()
                .system(AiSystemPrompts.AI_IMAGE_PRODUCT_EXTRACTOR)
                .user(userSpec -> userSpec
                        .text("""
                            User message:
                            %s

                            %s
                            """.formatted(prompt, converter.getFormat()))
                        .media(mimeType, imageResource))
                .call()
                .content();

        AiImageProductIntentDto result = converter.convert(content);
        return result != null ? result : new AiImageProductIntentDto();
    }

    private List<AiPaymentSummaryDto> resolvePayments(UUID userId, AiHelpIntentDto intent) {
        if ("PAYMENT_HELP".equalsIgnoreCase(intent.getHelpType()) ||
                "BOTH".equalsIgnoreCase(intent.getHelpType())) {

            if (intent.getPaymentReference() != null && !intent.getPaymentReference().isBlank()) {
                return aiOrderSupportService.getPaymentByReference(intent.getPaymentReference())
                        .map(List::of)
                        .orElse(List.of());
            }

            return aiOrderSupportService.getRecentPaymentsByUser(userId, 5);
        }

        return List.of();
    }

    private boolean shouldRecommendProducts(AiHelpIntentDto intent) {
        return "PRODUCT_HELP".equalsIgnoreCase(intent.getHelpType())
                || "PET_HEALTH".equalsIgnoreCase(intent.getHelpType())
                || "BOTH".equalsIgnoreCase(intent.getHelpType());
    }

    private boolean shouldRecommendServices(AiHelpIntentDto intent) {
        return "SERVICE_HELP".equalsIgnoreCase(intent.getHelpType())
                || "PET_HEALTH".equalsIgnoreCase(intent.getHelpType())
                || "BOTH".equalsIgnoreCase(intent.getHelpType())
                || intent.isEmergency();
    }

    private boolean needsOrderContext(AiHelpIntentDto intent) {
        return "ORDER_HELP".equalsIgnoreCase(intent.getHelpType())
                || "BOTH".equalsIgnoreCase(intent.getHelpType());
    }

    private String buildProductSearchText(AiHelpIntentDto intent) {
        if (intent.getProductKeywords() != null && !intent.getProductKeywords().isEmpty()) {
            return String.join(" ", intent.getProductKeywords());
        }

        if (intent.getKeywords() != null && !intent.getKeywords().isEmpty()) {
            return String.join(" ", intent.getKeywords());
        }

        return "";
    }

    private String resolveRecommendationType(List<ProductDto> products, List<ServiceProfileDto> services) {
        boolean hasProducts = products != null && !products.isEmpty();
        boolean hasServices = services != null && !services.isEmpty();

        if (hasProducts && hasServices) return "BOTH";
        if (hasProducts) return "PRODUCT";
        if (hasServices) return "SERVICE";
        return "NONE";
    }

    private List<String> buildFollowUps(AiHelpIntentDto intent) {
        if ("PET_HEALTH".equalsIgnoreCase(intent.getHelpType())) {
            return List.of(
                    "How long has your pet had these symptoms?",
                    "Has your pet eaten or drunk water normally?",
                    "Do you want nearby service providers for this issue?"
            );
        }

        if ("ORDER_HELP".equalsIgnoreCase(intent.getHelpType())) {
            return List.of(
                    "Do you want me to help check your recent order?",
                    "Do you have your order ID?",
                    "Do you want me to explain the next order step?"
            );
        }

        if ("PAYMENT_HELP".equalsIgnoreCase(intent.getHelpType())) {
            return List.of(
                    "Do you have the payment reference?",
                    "Do you want help checking the payment status?",
                    "Do you want me to explain what the payment result means?"
            );
        }

        return List.of(
                "Can you share a little more detail?",
                "Do you want product recommendations?",
                "Do you want nearby service recommendations?"
        );
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return null;
    }
}