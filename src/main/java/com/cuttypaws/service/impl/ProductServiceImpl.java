package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.exception.InvalidCredentialException;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.mapper.ProductMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.ProductResponse;
import com.cuttypaws.service.AwsS3Service;
import com.cuttypaws.service.interf.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
@RequiredArgsConstructor
//@CacheConfig(cacheNames = "products")
public class ProductServiceImpl implements ProductService {

    //    private final SearchService searchHistoryService;
    private final ProductRepo productRepo;
    private final UserRepo userRepo;
    private final SubCategoryRepo subCategoryRepo;
    private final CategoryRepo categoryRepo;
    private final ProductMapper productMapper;
    private final AwsS3Service awsS3Service;
    private final ServiceProfileRepo serviceProfileRepo;
    private final ServiceMediaRepo serviceMediaRepo;



    @Override
    @CacheEvict(value = { "mixed-feed",
            "products", "productById",
            "allCategories", "categoryById", "categoryWithSubCategories",
            "allSubCategories", "subCategoriesByCategory"
    }, allEntries = true)
    public ProductResponse createProduct(Long subCategoryId, List<MultipartFile> images, String name,
                                         String description, BigDecimal oldPrice,
                                         BigDecimal newPrice, List<String> sizes,
                                         List<String> colors, Integer stock) {
        log.info("🆕 Creating new product: {}", name);
        SubCategory subCategory = subCategoryRepo.findById(subCategoryId)
                .orElseThrow(() -> new NotFoundException("SubCategory not found"));

        Category category = subCategory.getCategory();
        if (category == null) {
            throw new NotFoundException("Category not found for the given subcategory");
        }

        Product product = new Product();
        product.setSubCategory(subCategory);
        product.setCategory(category);
        product.setName(name);
        product.setStock(stock);
        product.setDescription(description);
        product.setOldPrice(oldPrice);
        product.setNewPrice(newPrice);

        // Add sizes
        if (sizes != null) {
            sizes.forEach(size -> {
                ProductSize productSize = new ProductSize();
                productSize.setSize(size);
                productSize.setProduct(product);
                product.getSizes().add(productSize);
            });
        }

        // Add colors
        if (colors != null) {
            colors.forEach(color -> {
                ProductColor productColor = new ProductColor();
                productColor.setColor(color);
                productColor.setColorCode(productColor.getColorCode()); // Can be null
                productColor.setProduct(product);
                product.getColors().add(productColor);
            });
        }

        // Save product images
        List<ProductImage> productImages = images.stream()
                .map(image -> {
                    String imageUrl = awsS3Service.uploadMedia(image); // changed
                    ProductImage productImage = new ProductImage();
                    productImage.setImageUrl(imageUrl);
                    productImage.setProduct(product);
                    return productImage;
                })
                .collect(Collectors.toList());

        product.setImages(productImages);
        productRepo.save(product);

        log.info("✅ Product created successfully: {}", name);

        return ProductResponse.builder()
                .status(200)
                .timeStamp(LocalDateTime.now())
                .message("Product created successfully")
                .build();
    }

    @Override
    public List<Product> searchProductsBySubCategory(Long subCategoryId) {
        return productRepo.findBySubCategoryId(subCategoryId);
    }

    @Override
    @CacheEvict(value = { "mixed-feed",
            "products", "productById",
            "allCategories", "categoryById", "categoryWithSubCategories",
            "allSubCategories", "subCategoriesByCategory"
    }, allEntries = true)
    public ProductResponse updateProduct(Long productId, Long subCategoryId, List<MultipartFile> images, String name, String description, BigDecimal oldPrice, BigDecimal newPrice, List<String> sizes, List<String> colors, Integer stock) {
        log.info("🔄 Updating product {} and clearing caches", productId);

        // Fetch the product
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        // Update subcategory if provided
        if (subCategoryId != null) {
            SubCategory subCategory = subCategoryRepo.findById(subCategoryId)
                    .orElseThrow(() -> new NotFoundException("SubCategory not found"));

            // Fetch the category associated with the new subcategory
            Category category = subCategory.getCategory();
            if (category == null) {
                throw new NotFoundException("Category not found for the given subcategory");
            }

            // Update the product's subcategory and category
            product.setSubCategory(subCategory);
            product.setCategory(category);
        }

        // Update name if provided
        if (name != null) {
            product.setName(name);
        }

        // Update description if provided
        if (description != null) {
            product.setDescription(description);
        }

        // Update oldPrice if provided
        if (oldPrice != null) {
            product.setOldPrice(oldPrice);
        }
        if (newPrice != null) {
            product.setNewPrice(newPrice);
        }

        // Update sizes if provided
        if (sizes != null) {
            // Clear existing sizes
            product.getSizes().clear();

            // Add new sizes
            sizes.forEach(size -> {
                ProductSize productSize = new ProductSize();
                productSize.setSize(size);
                productSize.setProduct(product);
                product.getSizes().add(productSize);
            });
        }

        // Update colors if provided
        if (colors != null) {
            // Clear existing colors
            product.getColors().clear();

            // Add new colors
            colors.forEach(color -> {
                ProductColor productColor = new ProductColor();
                productColor.setColor(color);
                productColor.setProduct(product);
                product.getColors().add(productColor);
            });
        }

        // Update images if provided
        if (images != null && !images.isEmpty()) {
            product.getImages().clear();
            List<ProductImage> productImages = images.stream()
                    .map(image -> {
                        String imageUrl = awsS3Service.uploadMedia(image);  // changed
                        ProductImage productImage = new ProductImage();
                        productImage.setImageUrl(imageUrl);
                        productImage.setProduct(product);
                        return productImage;
                    })
                    .collect(Collectors.toList());

            product.getImages().addAll(productImages);
        }

        // Save the updated product
        productRepo.save(product);


        log.info("✅ Product updated and caches cleared for productId: {}", productId);

        return ProductResponse.builder()
                .status(200)
                .message("Product updated successfully")
                .build();
    }

    @Override
    @CacheEvict(value = { "mixed-feed",
            "products", "productById",
            "allCategories", "categoryById", "categoryWithSubCategories",
            "allSubCategories", "subCategoriesByCategory"
    }, allEntries = true)
    public ProductResponse deleteProduct(Long productId) {
        log.info("🗑️ Deleting product {} and clearing caches", productId);
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        productRepo.delete(product);

        log.info("✅ Product deleted and caches cleared for productId: {}", productId);

        return ProductResponse.builder()
                .status(200)
                .message("Product deleted successfully")
                .build();
    }

    @Override
    @Cacheable(value = "productById", key = "#productId",
            condition = "@cacheToggleService.isEnabled()",
            unless = "#result == null || #result.product == null")
    public ProductResponse getProductById(Long productId) {
        log.info("🔍 [CACHE MISS] Fetching product {} from database", productId);

        long startTime = System.currentTimeMillis();


        Optional<Product> productOptional = productRepo.findById(productId);
        if (productOptional.isEmpty()) {
            throw new NotFoundException("Product not found");
        }
        Product product = productOptional.get();
        ProductDto productDto = productMapper.mapProductToDtoBasic(product);

        long duration = System.currentTimeMillis() - startTime;

        log.info("📦 Retrieved product: {} from database in {}ms (CACHE MISS)", product.getName(), duration);

        ProductResponse response =  ProductResponse.builder()
                .status(200)
                .product(productDto)
                .build();
        if(response.isEmpty()){
            return response;
        }
        return response;
    }
    @Override
    @Cacheable(
            value = "productDetails",
            key = "#productId",
            condition = "@cacheToggleService.isEnabled()",
            unless = "#result == null || #result.productDetails == null"
    )
    public ProductResponse getProductDetails(Long productId) {

        long startTime = System.currentTimeMillis();

        log.info("Fetching product details for productId={}", productId);

        Product product = productRepo.findProductDetailsById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        log.info("Main product loaded: id={}, name={}, categoryId={}, subCategoryId={}",
                product.getId(),
                product.getName(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getSubCategory() != null ? product.getSubCategory().getId() : null
        );

        // Load collections safely
        Product productImages = productRepo.findProductWithImages(productId).orElse(product);
        Product productColors = productRepo.findProductWithColors(productId).orElse(product);
        Product productSizes = productRepo.findProductWithSizes(productId).orElse(product);

        product.setImages(productImages.getImages());
        product.setColors(productColors.getColors());
        product.setSizes(productSizes.getSizes());

        log.info("Images loaded: {}", product.getImages().size());
        log.info("Colors loaded: {}", product.getColors().size());
        log.info("Sizes loaded: {}", product.getSizes().size());

        ProductDto productDto = productMapper.mapProductToDtoBasic(product);

        List<ProductDto> relatedProducts = List.of();
        List<ProductDto> otherRelatedProducts = List.of();

        // SAME SUBCATEGORY PRODUCTS
        if (product.getSubCategory() != null) {

            log.info("Searching related products in subCategoryId={}",
                    product.getSubCategory().getId());

            List<Product> rawRelatedProducts = productRepo
                    .findRelatedProductsBySubCategory(
                            product.getSubCategory().getId(),
                            product.getId(),
                            PageRequest.of(0, 12)
                    );

            log.info("Raw related products returned: {}",
                    rawRelatedProducts.stream().map(Product::getId).toList());

            relatedProducts = rawRelatedProducts.stream()
                    .map(productMapper::mapProductToDtoBasic)
                    .toList();

            log.info("Mapped relatedProducts count={}", relatedProducts.size());
        }

        // SAME CATEGORY PRODUCTS
        if (product.getCategory() != null) {

            log.info("Searching other related products in categoryId={}",
                    product.getCategory().getId());

            List<ProductDto> rawCategoryProducts =
                    productRepo.findOtherRelatedProductsByCategory(
                                    product.getCategory().getId(),
                                    product.getId(),
                                    PageRequest.of(0, 20)
                            )
                            .stream()
                            .map(productMapper::mapProductToDtoBasic)
                            .toList();

            log.info("Raw category products returned ids={}",
                    rawCategoryProducts.stream().map(ProductDto::getId).toList());

            Set<Long> relatedIds = relatedProducts.stream()
                    .map(ProductDto::getId)
                    .collect(Collectors.toSet());

            log.info("Related product IDs removed from category list={}", relatedIds);

            otherRelatedProducts = rawCategoryProducts.stream()
                    .filter(p -> !relatedIds.contains(p.getId()))
                    .limit(12)
                    .toList();

            log.info("Final otherRelatedProducts count={}", otherRelatedProducts.size());
        }

        ProductDetailsDto productDetailsDto = ProductDetailsDto.builder()
                .product(productDto)
                .relatedProducts(relatedProducts)
                .otherRelatedProducts(otherRelatedProducts)
                .build();

        long duration = System.currentTimeMillis() - startTime;

        log.info("Product details loaded in {}ms | relatedProducts={} | otherRelatedProducts={}",
                duration,
                relatedProducts.size(),
                otherRelatedProducts.size()
        );

        return ProductResponse.builder()
                .status(200)
                .message("Product details fetched successfully")
                .timeStamp(LocalDateTime.now())
                .productDetails(productDetailsDto)
                .build();
    }

    @Cacheable(
            value = "products",
            key = "#page + ':' + #size",
            condition = "@cacheToggleService.isEnabled()",
            unless = "#result == null || #result.productList == null || #result.productList.isEmpty()"
    )
    @Override
    public ProductResponse getAllProduct(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);

        long startTime = System.currentTimeMillis();

        List<ProductDto> productList = productRepo.findProductCards(
                        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"))
                ).stream()
                .map(productMapper::mapProductToCardDto)
                .toList();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Retrieved {} products in {}ms", productList.size(), duration);

        return ProductResponse.builder()
                .status(200)
                .productList(productList)
                .timeStamp(LocalDateTime.now())
                .build();
    }

    @Override
    public ProductResponse getProductByCategory(Long categoryId) {
        Category category = categoryRepo.findById(categoryId)

                .orElseThrow(() -> new NotFoundException("Category not found"));
        List<ProductDto> productDtos = category.getSubCategories().stream()
                .flatMap(subCategory -> subCategory.getProducts().stream())
                .map(productMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());
        return ProductResponse.builder()
                .status(200)
                .productList(productDtos)
                .build();
    }

    @Override
    public ProductResponse searchProduct(String searchValue, Long userId, Long categoryId) {
        List<Product> products = productRepo.findTop8ByNameContainingIgnoreCase(searchValue);
        if (products.isEmpty()) {
            throw new NotFoundException("Product not found");
        }
        List<ProductDto> productDtoList = products.stream()
                .map(productMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .productList(productDtoList)
                .build();
    }

    @Override
    public ProductResponse getProductsByNameAndCategory(String name, Long categoryId) {
        List<Product> products = productRepo.findByNameAndCategoryId(name, categoryId);
        if (products.isEmpty()) {
            throw new NotFoundException("No products found with the given name and category");
        }
        List<ProductDto> productDtos = products.stream()
                .map(productMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());
        return ProductResponse.builder()
                .status(200)
                .productList(productDtos)
                .build();
    }


    @Override
    public ProductResponse searchProductsWithPrice(String searchQuery, Long categoryId, Long userId) {
        Map<String, Object> parsedQuery = parseSearchQuery(searchQuery);
        String name = (String) parsedQuery.get("name");
        BigDecimal minPrice = (BigDecimal) parsedQuery.get("minPrice");
        BigDecimal maxPrice = (BigDecimal) parsedQuery.get("maxPrice");
        List<Product> products = productRepo.searchProducts(name, categoryId, minPrice, maxPrice);
        if (products.isEmpty()) {
            throw new NotFoundException("No products found with the given criteria");
        }
        List<ProductDto> productDtos = products.stream()
                .map(productMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .status(200)
                .productList(productDtos)
                .build();
    }

    private Map<String, Object> parseSearchQuery(String searchQuery) {
        Map<String, Object> result = new HashMap<>();
        String name = searchQuery.trim(); // Default name is the entire query
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        try {
            // Convert the search query to lowercase for case-insensitive comparison
            String lowerCaseQuery = searchQuery.toLowerCase();

            // Check for price-related keywords in the query
            if (lowerCaseQuery.contains("under")) {
                String[] parts = lowerCaseQuery.split("under");
                if (parts.length > 1) {
                    name = parts[0].trim();
                    maxPrice = parsePrice(parts[1]);
                }
            } else if (lowerCaseQuery.contains("over")) {
                String[] parts = lowerCaseQuery.split("over");
                if (parts.length > 1) {
                    name = parts[0].trim();
                    minPrice = parsePrice(parts[1]);
                }
            } else if (lowerCaseQuery.contains("greater than")) {
                String[] parts = lowerCaseQuery.split("greater than");
                if (parts.length > 1) {
                    name = parts[0].trim();
                    minPrice = parsePrice(parts[1]);
                }
            } else if (lowerCaseQuery.contains("less than")) {
                String[] parts = lowerCaseQuery.split("less than");
                if (parts.length > 1) {
                    name = parts[0].trim();
                    maxPrice = parsePrice(parts[1]);
                }
            } else if (searchQuery.contains("$")) {
                String[] parts = searchQuery.split("\\$");
                if (parts.length > 1) {
                    name = parts[0].trim();
                    maxPrice = parsePrice(parts[1]);
                    minPrice = maxPrice; // Exact price match
                }
            } else if (searchQuery.contains("-")) {
                String[] parts = searchQuery.split("-");
                if (parts.length == 2) {
                    name = parts[0].trim();
                    minPrice = parsePrice(parts[0]);
                    maxPrice = parsePrice(parts[1]);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("error");
        }
        result.put("name", name);
        result.put("minPrice", minPrice);
        result.put("maxPrice", maxPrice);

        return result;
    }

    private BigDecimal parsePrice(String priceString) {
        try {
            String cleanedPrice = priceString.replaceAll("[^0-9.]", "").trim();
            if (cleanedPrice.isEmpty()) {
                return null;
            }
            if (cleanedPrice.chars().filter(ch -> ch == '.').count() > 1) {
                return null;
            }
            return new BigDecimal(cleanedPrice);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public ProductResponse getAllProductBySubCategory(Long subCategoryId) {
        SubCategory subCategory = subCategoryRepo.findById(subCategoryId)
                .orElseThrow(() -> new NotFoundException("SubCategory not found"));

        List<ProductDto> productDtos = subCategory.getProducts().stream()
                .map(productMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .status(200)
                .productList(productDtos)
                .build();
    }

    @Override
    public ProductResponse getTrendingProducts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        Page<Product> productPage = productRepo.findTrendingProducts(
                cutoff,
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "viewCount"))
        );
        List<ProductDto> productDtos = productPage.getContent().stream()
                .map(productMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .status(200)
                .trendingProducts(productDtos)
                .build();
    }

    @Override
    @Transactional
    public void trackProductView(Long productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        product.setViewCount((product.getViewCount() != null ? product.getViewCount() : 0) + 1);
        product.setLastViewedDate(LocalDateTime.now());
        productRepo.save(product);
    }

    // In ProductServiceImpl.java

    @Override
    @Transactional
    public ProductResponse likeProduct(Long productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        Integer currentLikes = product.getLikes() != null ? product.getLikes() : 0;
        product.setLikes(currentLikes + 1);
        try {
            productRepo.save(product);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update product likes: " + e.getMessage(), e);
        }
        return ProductResponse.builder()
                .status(200)
                .message("Product liked successfully")
                .build();
    }

    @Override
    public ProductResponse getAllProductsWithLikes() {
        List<Product> products = productRepo.findAllWithLikes();
        List<ProductDto> productDtos = products.stream()
                .map(productMapper::mapProductToDtoBasic) // Assuming you have a mapper method
                .collect(Collectors.toList());
        return ProductResponse.builder()
                .status(200)
                .message("Products fetched successfully")
                .productList(productDtos)
                .build();
    }

    @Override
    @CacheEvict(value = {
            "mixed-feed",
            "products", "productById",
            "allCategories", "categoryById", "categoryWithSubCategories",
            "allSubCategories", "subCategoriesByCategory"
    }, allEntries = true)
    public ProductResponse createProductForCompany(UUID companyId, Long subCategoryId, List<MultipartFile> images, String name, String description, BigDecimal oldPrice, BigDecimal newPrice, List<String> sizes, List<String> colors, Integer stock) {
        User company = userRepo.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        SubCategory subCategory = subCategoryRepo.findById(subCategoryId)
                .orElseThrow(() -> new NotFoundException("SubCategory not found"));

        Category category = subCategory.getCategory();
        if (category == null) {
            throw new NotFoundException("Category not found for the given subcategory");
        }

        Product product = new Product();
        product.setUser(company);
        product.setSubCategory(subCategory);
        product.setCategory(category);
        product.setName(name);
        product.setStock(stock);
        product.setDescription(description);
        product.setOldPrice(oldPrice);
        product.setNewPrice(newPrice);

        // Add sizes
        if (sizes != null) {
            sizes.forEach(size -> {
                ProductSize productSize = new ProductSize();
                productSize.setSize(size);
                productSize.setProduct(product);
                product.getSizes().add(productSize);
            });
        }

        // Add colors
        if (colors != null) {
            colors.forEach(color -> {
                ProductColor productColor = new ProductColor();
                productColor.setColor(color);
                productColor.setProduct(product);
                product.getColors().add(productColor);
            });
        }

        // Save product images
        List<ProductImage> productImages = images.stream()
                .map(image -> {
                    String imageUrl = awsS3Service.uploadMedia(image);
                    ProductImage productImage = new ProductImage();
                    productImage.setImageUrl(imageUrl);
                    productImage.setProduct(product);
                    return productImage;
                })
                .collect(Collectors.toList());
        product.setImages(productImages);

        productRepo.save(product);
        return ProductResponse.builder()
                .status(200)
                .message("Product created successfully for company: " + company.getName())
                .build();
    }

    @Override
    @CacheEvict(value = {
            "mixed-feed",
            "products", "productById",
            "allCategories", "categoryById", "categoryWithSubCategories",
            "allSubCategories", "subCategoriesByCategory"
    }, allEntries = true)
    public ProductResponse updateProductForCompany(Long productId, UUID companyId, Long subCategoryId, List<MultipartFile> images, String name, String description, BigDecimal oldPrice, BigDecimal newPrice, List<String> sizes, List<String> colors, Integer stock) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        User company = userRepo.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        if (!product.getUser().getId().equals(company.getId())) {
            throw new InvalidCredentialException("You are not authorized to update this product.");
        }

        if (subCategoryId != null) {
            SubCategory subCategory = subCategoryRepo.findById(subCategoryId)
                    .orElseThrow(() -> new NotFoundException("SubCategory not found"));

            // Fetch the category associated with the new subcategory
            Category category = subCategory.getCategory();
            if (category == null) {
                throw new NotFoundException("Category not found for the given subcategory");
            }

            // Update the product's subcategory and category
            product.setSubCategory(subCategory);
            product.setCategory(category);
        }

        // Update name if provided
        if (name != null) {
            product.setName(name);
        }

        // Update description if provided
        if (description != null) {
            product.setDescription(description);
        }

        // Update oldPrice if provided
        if (oldPrice != null) {
            product.setOldPrice(oldPrice);
        }
        if(stock != null){
            product.setStock(stock);
        }
        if (newPrice != null) {
            product.setNewPrice(newPrice);
        }

        // Update sizes if provided
        if (sizes != null) {
            // Clear existing sizes
            product.getSizes().clear();

            // Add new sizes
            sizes.forEach(size -> {
                ProductSize productSize = new ProductSize();
                productSize.setSize(size);
                productSize.setProduct(product);
                product.getSizes().add(productSize);
            });
        }

        // Update colors if provided
        if (colors != null) {
            // Clear existing colors
            product.getColors().clear();

            // Add new colors
            colors.forEach(color -> {
                ProductColor productColor = new ProductColor();
                productColor.setColor(color);
                productColor.setProduct(product);
                product.getColors().add(productColor);
            });
        }

        // Update images if provided
        if (images != null && !images.isEmpty()) {
            // Clear existing images
            product.getImages().clear();

            // Add new images
            List<ProductImage> productImages = images.stream()
                    .map(image -> {
                        String imageUrl = awsS3Service.uploadMedia(image);  // changed
                        ProductImage productImage = new ProductImage();
                        productImage.setImageUrl(imageUrl);
                        productImage.setProduct(product);
                        return productImage;
                    })
                    .collect(Collectors.toList());

            product.getImages().addAll(productImages);
        }
        productRepo.save(product);
        return ProductResponse.builder()
                .status(200)
                .message("Product updated successfully for company: " + company.getName())
                .build();
    }
    @Override
    public ProductResponse getAllProductsByUser(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        List<Product> products = productRepo.findByUserId(userId);
        List<ProductDto> productDtos = products.stream()
                .map(productMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());
        return ProductResponse.builder()
                .status(200)
                .productList(productDtos)
                .build();
    }

    @Override
    @CacheEvict(value = {
            "mixed-feed",
            "products", "productById",
            "allCategories", "categoryById", "categoryWithSubCategories",
            "allSubCategories", "subCategoriesByCategory"
    }, allEntries = true)
    public ProductResponse deleteProductForCompany(Long productId, UUID companyId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        User company = userRepo.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        if (!product.getUser().getId().equals(company.getId())) {
            throw new InvalidCredentialException("You are not authorized to delete this product.");
        }

        productRepo.delete(product);
        return ProductResponse.builder()
                .status(200)
                .message("Product deleted successfully for company: " + company.getName())
                .build();
    }

    @Override
    public ProductResponse getSearchSuggestions(String query) {
        if (query == null || query.trim().length() < 2) {
            return ProductResponse.builder()
                    .status(200)
                    .suggestions(List.of())
                    .build();
        }

        String cleanedQuery = query.trim();

        List<Product> products = productRepo.findTop8ByNameContainingIgnoreCase(cleanedQuery);
        List<Category> categories = categoryRepo.searchCategories(cleanedQuery);
        List<SubCategory> subCategories = subCategoryRepo.searchSubCategories(cleanedQuery);

        List<ServiceProfile> servicesByName =
                serviceProfileRepo.findTop8ByStatusAndBusinessNameContainingIgnoreCaseOrderByCreatedAtDesc(
                        com.cuttypaws.enums.ServiceStatus.ACTIVE,
                        cleanedQuery
                );

        List<ServiceProfile> servicesByDescription =
                serviceProfileRepo.findTop8ByStatusAndDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(
                        com.cuttypaws.enums.ServiceStatus.ACTIVE,
                        cleanedQuery
                );

        List<ServiceProfile> servicesByCity =
                serviceProfileRepo.findTop8ByStatusAndCityContainingIgnoreCaseOrderByCreatedAtDesc(
                        com.cuttypaws.enums.ServiceStatus.ACTIVE,
                        cleanedQuery
                );

        List<SearchSuggestionDto> productSuggestions = products.stream()
                .limit(8)
                .map(p -> SearchSuggestionDto.builder()
                        .id(String.valueOf(p.getId()))
                        .routeId(String.valueOf(p.getId()))
                        .name(p.getName())
                        .type("product")
                        .imageUrl(
                                p.getImages() != null && !p.getImages().isEmpty()
                                        ? p.getImages().get(0).getImageUrl()
                                        : null
                        )
                        .category(p.getCategory() != null ? p.getCategory().getName() : "uncategorized")
                        .subCategory(p.getSubCategory() != null ? p.getSubCategory().getName() : "uncategorized")
                        .build())
                .toList();

        List<SearchSuggestionDto> categorySuggestions = categories.stream()
                .limit(5)
                .map(c -> SearchSuggestionDto.builder()
                        .id(String.valueOf(c.getId()))
                        .routeId(String.valueOf(c.getId()))
                        .name(c.getName())
                        .type("category")
                        .build())
                .toList();

        List<SearchSuggestionDto> subCategorySuggestions = subCategories.stream()
                .limit(5)
                .map(s -> SearchSuggestionDto.builder()
                        .id(String.valueOf(s.getId()))
                        .routeId(String.valueOf(s.getId()))
                        .name(s.getName())
                        .type("subcategory")
                        .parentCategory(s.getCategory() != null ? s.getCategory().getName() : null)
                        .build())
                .toList();

        List<SearchSuggestionDto> serviceSuggestions = Stream.of(
                        servicesByName.stream(),
                        servicesByDescription.stream(),
                        servicesByCity.stream()
                )
                .flatMap(stream -> stream)
                .collect(Collectors.toMap(
                        ServiceProfile::getId,
                        service -> service,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(8)
                .map(service -> {
                    List<ServiceMedia> mediaList = serviceMediaRepo.findByServiceProfileIdOrderByDisplayOrderAsc(service.getId());

                    String coverImageUrl = mediaList.stream()
                            .filter(item -> Boolean.TRUE.equals(item.getIsCover()))
                            .map(ServiceMedia::getMediaUrl)
                            .findFirst()
                            .orElse(mediaList.isEmpty()
                                    ? "http://localhost:9393/images/cuttypaws-logo.png"
                                    : mediaList.get(0).getMediaUrl());

                    return SearchSuggestionDto.builder()
                            .id(service.getId() != null ? service.getId().toString() : null)
                            .routeId(
                                    service.getUser() != null && service.getUser().getId() != null
                                            ? service.getUser().getId().toString()
                                            : null
                            )
                            .name(
                                    service.getBusinessName() != null && !service.getBusinessName().isBlank()
                                            ? service.getBusinessName()
                                            : "Service Provider"
                            )
                            .type("service")
                            .imageUrl(coverImageUrl)
                            .serviceType(service.getServiceType() != null ? service.getServiceType().name() : null)
                            .city(service.getCity())
                            .state(service.getState())
                            .build();
                })
                .toList();

        List<SearchSuggestionDto> suggestions = Stream.of(
                        productSuggestions.stream(),
                        serviceSuggestions.stream(),
                        categorySuggestions.stream(),
                        subCategorySuggestions.stream()
                )
                .flatMap(stream -> stream)
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .status(200)
                .suggestions(suggestions)
                .build();
    }

}