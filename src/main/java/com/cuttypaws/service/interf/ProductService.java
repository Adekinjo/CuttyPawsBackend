package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;

import com.cuttypaws.entity.Product;
import com.cuttypaws.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<ProductDto> searchProducts(String query);

    ProductResponse createProduct(Long subCategoryId, List<MultipartFile> images, String name, String description, BigDecimal oldPrice, BigDecimal newPrice, List<String> sizes, List<String> colors, Integer stock);

    ProductResponse updateProduct(Long productId, Long subCategory,  List<MultipartFile> images, String name, String description, BigDecimal price, BigDecimal newPrice, List<String> sizes, List<String> colors, Integer stock);

    ProductResponse deleteProduct(Long productId);

    ProductResponse getProductById(Long productId);

    ProductResponse getAllProduct();

    List<ProductDto> getRelatedProducts(String query);

    ProductResponse getProductByCategory(Long categoryId);

    ProductResponse searchProduct(String searchValue, Long userId, Long categoryId);

    ProductResponse getProductSuggestions(String query);

    ProductResponse getProductsByNameAndCategory(String name, Long categoryId);

    ProductResponse searchProductsWithPrice(String query, Long categoryId, Long userId);

    List<Product> searchProductsBySubCategory(Long subCategoryId);

    ProductResponse getAllProductBySubCategory(Long subCategoryId);

    ProductResponse getAllProductsWithLikes();

    ProductResponse getTrendingProducts();
    void trackProductView(Long productId);

    ProductResponse likeProduct(Long productId);

    ProductResponse createProductForCompany(UUID companyId, Long subCategoryId, List<MultipartFile> images, String name, String description, BigDecimal oldPrice, BigDecimal newPrice, List<String> sizes, List<String> colors, Integer stock);

    ProductResponse updateProductForCompany(Long productId, UUID companyId, Long subCategoryId, List<MultipartFile> images, String name, String description, BigDecimal oldPrice, BigDecimal newPrice, List<String> sizes, List<String> colors, Integer stock);

    ProductResponse deleteProductForCompany(Long productId, UUID companyId);

    ProductResponse getAllProductsByUser(UUID userId);

    ProductResponse getSearchSuggestions(String query);

}

