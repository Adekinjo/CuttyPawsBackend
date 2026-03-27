//package com.cuttypaws.ai.service.impl;
//
//import com.cuttypaws.ai.dto.ExternalProductSuggestionDto;
//import com.cuttypaws.ai.service.interf.AiExternalProductSuggestionService;
//import com.cuttypaws.ai.service.interf.AiRecommendationService;
//import com.cuttypaws.dto.ProductDto;
//import com.cuttypaws.dto.ServiceMediaDto;
//import com.cuttypaws.dto.ServiceProfileDto;
//import com.cuttypaws.entity.Product;
//import com.cuttypaws.entity.ServiceProfile;
//import com.cuttypaws.enums.ServiceStatus;
//import com.cuttypaws.enums.ServiceType;
//import com.cuttypaws.mapper.ProductMapper;
//import com.cuttypaws.mapper.ServiceProviderMapper;
//import com.cuttypaws.repository.ProductRepo;
//import com.cuttypaws.repository.ServiceMediaRepo;
//import com.cuttypaws.repository.ServiceProfileRepo;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//
//@Service
//@RequiredArgsConstructor
//public class AiRecommendationServiceImpl implements AiRecommendationService {
//
//    private final ProductRepo productRepo;
//    private final ProductMapper productMapper;
//    private final ServiceProfileRepo serviceProfileRepo;
//    private final ServiceMediaRepo serviceMediaRepo;
//    private final ServiceProviderMapper serviceProviderMapper;
//    private final AiExternalProductSuggestionService aiExternalProductSuggestionService;
//
//    @Override
//    public List<ProductDto> recommendProducts(String query) {
//        if (query == null || query.isBlank()) {
//            return Collections.emptyList();
//        }
//
//        String normalized = normalizeSearchPhrase(query);
//
//        List<Product> exactMatches = productRepo.searchBroadProductTerm(normalized);
//
//        if (!exactMatches.isEmpty()) {
//            return exactMatches.stream()
//                    .limit(6)
//                    .map(productMapper::mapProductToDtoBasic)
//                    .toList();
//        }
//
//        List<String> tokens = tokenize(normalized);
//
//        List<Product> merged = new java.util.ArrayList<>();
//
//        for (String token : tokens) {
//            if (token.length() < 2) {
//                continue;
//            }
//            merged.addAll(productRepo.searchBroadProductTerm(token));
//        }
//
//        return merged.stream()
//                .collect(java.util.stream.Collectors.toMap(
//                        Product::getId,
//                        p -> p,
//                        (a, b) -> a
//                ))
//                .values()
//                .stream()
//                .sorted((a, b) -> scoreProduct(b, tokens) - scoreProduct(a, tokens))
//                .limit(6)
//                .map(productMapper::mapProductToDtoBasic)
//                .toList();
//    }
//
//    private String normalizeSearchPhrase(String query) {
//        return query == null ? "" : query.toLowerCase()
//                .replace("do you have", "")
//                .replace("can i get", "")
//                .replace("i need", "")
//                .replace("please", "")
//                .replace("product", "")
//                .replace("products", "")
//                .replaceAll("\\s+", " ")
//                .trim();
//    }
//
//    private List<String> tokenize(String text) {
//        if (text == null || text.isBlank()) {
//            return List.of();
//        }
//
//        return java.util.Arrays.stream(text.split("\\s+"))
//                .map(String::trim)
//                .filter(token -> !token.isBlank())
//                .toList();
//    }
//
//    private int scoreProduct(Product product, List<String> tokens) {
//        int score = 0;
//
//        String name = product.getName() != null ? product.getName().toLowerCase() : "";
//        String description = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
//        String category = product.getCategory() != null && product.getCategory().getName() != null
//                ? product.getCategory().getName().toLowerCase()
//                : "";
//        String subCategory = product.getSubCategory() != null && product.getSubCategory().getName() != null
//                ? product.getSubCategory().getName().toLowerCase()
//                : "";
//
//        for (String token : tokens) {
//            if (name.contains(token)) score += 5;
//            if (description.contains(token)) score += 3;
//            if (category.contains(token)) score += 2;
//            if (subCategory.contains(token)) score += 2;
//        }
//
//        return score;
//    }
//
//    @Override
//    public List<ServiceProfileDto> recommendServices(String serviceType, String city, String state) {
//        List<ServiceProfile> profiles;
//        boolean hasCity = city != null && !city.isBlank();
//        boolean hasState = state != null && !state.isBlank();
//
//        if (serviceType != null && !serviceType.isBlank()) {
//            try {
//                ServiceType parsedType = ServiceType.valueOf(serviceType.toUpperCase());
//
//                if (hasCity && hasState) {
//                    profiles = serviceProfileRepo.findByStatusAndServiceTypeAndCityIgnoreCaseAndStateIgnoreCase(
//                            ServiceStatus.ACTIVE, parsedType, city, state, PageRequest.of(0, 6)
//                    );
//                } else {
//                    profiles = serviceProfileRepo.findByStatusAndServiceTypeOrderByCreatedAtDesc(
//                            ServiceStatus.ACTIVE, parsedType, PageRequest.of(0, 6)
//                    );
//                }
//            } catch (IllegalArgumentException e) {
//                profiles = hasCity && hasState
//                        ? serviceProfileRepo.findByStatusAndCityIgnoreCaseAndStateIgnoreCase(
//                        ServiceStatus.ACTIVE, city, state, PageRequest.of(0, 6))
//                        : serviceProfileRepo.findByStatusOrderByCreatedAtDesc(
//                        ServiceStatus.ACTIVE, PageRequest.of(0, 6));
//            }
//        } else {
//            profiles = hasCity && hasState
//                    ? serviceProfileRepo.findByStatusAndCityIgnoreCaseAndStateIgnoreCase(
//                    ServiceStatus.ACTIVE, city, state, PageRequest.of(0, 6))
//                    : serviceProfileRepo.findByStatusOrderByCreatedAtDesc(
//                    ServiceStatus.ACTIVE, PageRequest.of(0, 6));
//        }
//
//        if (profiles.isEmpty()) return List.of();
//
//        List<UUID> profileIds = profiles.stream().map(ServiceProfile::getId).toList();
//
//        Map<UUID, List<ServiceMediaDto>> mediaByProfileId = serviceMediaRepo
//                .findByServiceProfileIdInOrderByDisplayOrderAsc(profileIds)
//                .stream()
//                .map(item -> ServiceMediaDto.builder()
//                        .id(item.getId())
//                        .mediaType(item.getMediaType().name())
//                        .mediaUrl(item.getMediaUrl())
//                        .isCover(item.getIsCover())
//                        .build())
//                .collect(java.util.stream.Collectors.groupingBy(ServiceMediaDto::getServiceProfileId)); // or group by entity id before mapping
//
//        return profiles.stream()
//                .map(profile -> serviceProviderMapper.toDto(
//                        profile,
//                        mediaByProfileId.getOrDefault(profile.getId(), List.of())
//                ))
//                .toList();
//    }
//
//
//    @Override
//    public List<ExternalProductSuggestionDto> recommendExternalProducts(List<String> keywords) {
//        return aiExternalProductSuggestionService.searchExternalProducts(keywords);
//    }
//}







package com.cuttypaws.ai.service.impl;

import com.cuttypaws.ai.dto.ExternalProductSuggestionDto;
import com.cuttypaws.ai.service.interf.AiExternalProductSuggestionService;
import com.cuttypaws.ai.service.interf.AiRecommendationService;
import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.dto.ServiceMediaDto;
import com.cuttypaws.dto.ServiceProfileDto;
import com.cuttypaws.entity.Product;
import com.cuttypaws.entity.ServiceProfile;
import com.cuttypaws.enums.ServiceStatus;
import com.cuttypaws.enums.ServiceType;
import com.cuttypaws.mapper.ProductMapper;
import com.cuttypaws.mapper.ServiceProviderMapper;
import com.cuttypaws.repository.ProductRepo;
import com.cuttypaws.repository.ServiceMediaRepo;
import com.cuttypaws.repository.ServiceProfileRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final ProductRepo productRepo;
    private final ProductMapper productMapper;
    private final ServiceProfileRepo serviceProfileRepo;
    private final ServiceMediaRepo serviceMediaRepo;
    private final ServiceProviderMapper serviceProviderMapper;
    private final AiExternalProductSuggestionService aiExternalProductSuggestionService;

    @Override
    @Cacheable(
            value = "aiProductRecommendations",
            key = "#query == null ? '' : #query.toLowerCase().trim()"
    )
    public List<ProductDto> recommendProducts(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String normalized = normalizeSearchPhrase(query);

        List<Product> exactMatches = productRepo.searchBroadProductTerm(normalized);
        if (!exactMatches.isEmpty()) {
            return exactMatches.stream()
                    .limit(6)
                    .map(productMapper::mapProductToDtoBasic)
                    .toList();
        }

        List<String> tokens = tokenize(normalized);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<Product> merged = new ArrayList<>();

        for (String token : tokens) {
            merged.addAll(productRepo.searchBroadProductTerm(token));
        }

        return merged.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> product,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted((a, b) -> Integer.compare(scoreProduct(b, tokens), scoreProduct(a, tokens)))
                .limit(6)
                .map(productMapper::mapProductToDtoBasic)
                .toList();
    }

    private String normalizeSearchPhrase(String query) {
        return query == null ? "" : query.toLowerCase()
                .replace("do you have", "")
                .replace("can i get", "")
                .replace("i need", "")
                .replace("please", "")
                .replace("product", "")
                .replace("products", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return java.util.Arrays.stream(text.split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .distinct()
                .limit(3)
                .toList();
    }

    private int scoreProduct(Product product, List<String> tokens) {
        int score = 0;

        String name = product.getName() != null ? product.getName().toLowerCase() : "";
        String description = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
        String category = product.getCategory() != null && product.getCategory().getName() != null
                ? product.getCategory().getName().toLowerCase()
                : "";
        String subCategory = product.getSubCategory() != null && product.getSubCategory().getName() != null
                ? product.getSubCategory().getName().toLowerCase()
                : "";

        for (String token : tokens) {
            if (name.contains(token)) score += 5;
            if (description.contains(token)) score += 3;
            if (category.contains(token)) score += 2;
            if (subCategory.contains(token)) score += 2;
        }

        return score;
    }

    @Override
    @Cacheable(
            value = "aiServiceRecommendations",
            key = "(#serviceType == null ? '' : #serviceType) + '|' + (#city == null ? '' : #city) + '|' + (#state == null ? '' : #state)"
    )
    public List<ServiceProfileDto> recommendServices(String serviceType, String city, String state) {
        List<ServiceProfile> profiles;
        boolean hasCity = city != null && !city.isBlank();
        boolean hasState = state != null && !state.isBlank();

        if (serviceType != null && !serviceType.isBlank()) {
            try {
                ServiceType parsedType = ServiceType.valueOf(serviceType.toUpperCase());

                if (hasCity && hasState) {
                    profiles = serviceProfileRepo.findByStatusAndServiceTypeAndCityIgnoreCaseAndStateIgnoreCase(
                            ServiceStatus.ACTIVE,
                            parsedType,
                            city,
                            state,
                            PageRequest.of(0, 6)
                    );
                } else {
                    profiles = serviceProfileRepo.findByStatusAndServiceTypeOrderByCreatedAtDesc(
                            ServiceStatus.ACTIVE,
                            parsedType,
                            PageRequest.of(0, 6)
                    );
                }
            } catch (IllegalArgumentException e) {
                profiles = hasCity && hasState
                        ? serviceProfileRepo.findByStatusAndCityIgnoreCaseAndStateIgnoreCase(
                        ServiceStatus.ACTIVE,
                        city,
                        state,
                        PageRequest.of(0, 6)
                )
                        : serviceProfileRepo.findByStatusOrderByCreatedAtDesc(
                        ServiceStatus.ACTIVE,
                        PageRequest.of(0, 6)
                );
            }
        } else {
            profiles = hasCity && hasState
                    ? serviceProfileRepo.findByStatusAndCityIgnoreCaseAndStateIgnoreCase(
                    ServiceStatus.ACTIVE,
                    city,
                    state,
                    PageRequest.of(0, 6)
            )
                    : serviceProfileRepo.findByStatusOrderByCreatedAtDesc(
                    ServiceStatus.ACTIVE,
                    PageRequest.of(0, 6)
            );
        }

        if (profiles.isEmpty()) {
            return List.of();
        }

        List<UUID> profileIds = profiles.stream()
                .map(ServiceProfile::getId)
                .toList();

        Map<UUID, List<ServiceMediaDto>> mediaByProfileId = serviceMediaRepo
                .findByServiceProfileIdInOrderByDisplayOrderAsc(profileIds)
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getServiceProfile().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                item -> ServiceMediaDto.builder()
                                        .id(item.getId())
                                        .mediaType(item.getMediaType().name())
                                        .mediaUrl(item.getMediaUrl())
                                        .isCover(item.getIsCover())
                                        .build(),
                                Collectors.toList()
                        )
                ));

        return profiles.stream()
                .map(profile -> serviceProviderMapper.toDto(
                        profile,
                        mediaByProfileId.getOrDefault(profile.getId(), List.of())
                ))
                .toList();
    }

    @Override
    public List<ExternalProductSuggestionDto> recommendExternalProducts(List<String> keywords) {
        return aiExternalProductSuggestionService.searchExternalProducts(keywords);
    }
}