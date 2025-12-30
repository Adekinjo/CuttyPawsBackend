package com.cuttypaws.mapper;

import com.cuttypaws.dto.ColorDto;
import com.cuttypaws.dto.DealDto;
import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.entity.Deal;
import com.cuttypaws.entity.Product;
import com.cuttypaws.entity.ProductImage;
import com.cuttypaws.entity.ProductSize;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DealMapper {


    public ProductDto mapProductToDtoBasic(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setOldPrice(product.getOldPrice());
        dto.setNewPrice(product.getNewPrice());
        dto.setThumbnailImageUrl(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());
        dto.setImageUrls(product.getImages().stream().map(ProductImage::getImageUrl).collect(Collectors.toList()));
        dto.setLikes(product.getLikes());
        dto.setStock(product.getStock());

        if (product.getUser() != null) {
            dto.setUserId(product.getUser().getId());
            dto.setCompanyName(
                    product.getUser().getCompanyName() != null ?
                            product.getUser().getCompanyName() :
                            "No Company"
            );
        } else {
            dto.setCompanyName("No Company");
        }

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategory(product.getCategory().getName()); // Add this line
        } else {
            dto.setCategory("uncategorized"); // Default value
        }
        if (product.getSubCategory() != null) {
            dto.setSubCategoryId(product.getSubCategory().getId());
            dto.setSubCategory(product.getSubCategory().getName()); // Add this line
        } else {
            dto.setSubCategory("uncategorized"); // Default value
        }
        dto.setSizes(product.getSizes().stream().map(ProductSize::getSize).collect(Collectors.toList()));

        // Map colors (with color name and color code)
        if (product.getColors() != null) {
            dto.setColors(product.getColors().stream()
                    .map(color -> new ColorDto(
                            color.getColor(),
                            color.getColorCode() != null ?
                                    color.getColorCode() :
                                    getDefaultColorCode(color.getColor()) // Use shared logic
                    ))
                    .collect(Collectors.toList()));
        }
        dto.setViewCount(product.getViewCount());
        dto.setLastViewedDate(product.getLastViewedDate());

        return dto;
    }


    private String getDefaultColorCode(String colorName) {
        Map<String, String> defaultColors = new HashMap<>();
        // Use lowercase keys for all color names
        defaultColors.put("red", "#FF0000");
        defaultColors.put("blue", "#0000FF");
        defaultColors.put("green", "#00FF00");
        defaultColors.put("black", "#000000");
        defaultColors.put("cyan", "#00FFFF");
        defaultColors.put("magenta", "#FF00FF");
        defaultColors.put("dark gray", "#404040");
        defaultColors.put("gray", "#808080");
        defaultColors.put("light gray", "#C0C0C0");
        defaultColors.put("purple", "#800080");
        defaultColors.put("pink", "#FFC0CB");
        defaultColors.put("brown", "#A52A2A");
        defaultColors.put("gold", "#FFD700");
        defaultColors.put("silver", "#C0C0C0");
        defaultColors.put("orange", "#FFA500");
        defaultColors.put("maroon", "#800000");
        defaultColors.put("tomato", "#FF6347");
        defaultColors.put("orange red", "#FF4500");
        defaultColors.put("chocolate", "#D2691E");
        defaultColors.put("lime green", "#32CD32");
        defaultColors.put("dark green", "#008000");
        defaultColors.put("navy blue", "#000080");
        defaultColors.put("olive", "#808000");
        defaultColors.put("green yellow", "#ADFF2F");
        defaultColors.put("deep sky blue", "#00BFFF");
        defaultColors.put("pale green", "#98FB98");
        defaultColors.put("spring green", "#00FF7F");
        defaultColors.put("light green", "#90EE90");
        defaultColors.put("light blue", "#ADD8E6");
        defaultColors.put("sky blue", "#87CEEB");
        defaultColors.put("powder blue", "#B0E0E6");
        defaultColors.put("light cyan", "#E0FFFF");
        defaultColors.put("baby blue", "#89CFF0");
        defaultColors.put("ashes", "#C6C3B5");
        defaultColors.put("white", "#FFFFFF");
        defaultColors.put("yellow", "#FFFF00");
        defaultColors.put("violet", "#EE82EE");
        defaultColors.put("indigo", "#4B0082");
        defaultColors.put("teal", "#008080");
        defaultColors.put("coral", "#FF7F50");
        defaultColors.put("salmon", "#FA8072");
        defaultColors.put("khaki", "#F0E68C");
        defaultColors.put("lavender", "#E6E6FA");
        defaultColors.put("plum", "#DDA0DD");
        defaultColors.put("turquoise", "#40E0D0");
        defaultColors.put("azure", "#F0FFFF");
        defaultColors.put("beige", "#F5F5DC");
        defaultColors.put("mint", "#98FF98");
        defaultColors.put("peach", "#FFDAB9");
        defaultColors.put("mustard", "#FFDB58");
        defaultColors.put("burgundy", "#800020");
        defaultColors.put("ruby", "#E0115F");
        defaultColors.put("emerald", "#50C878");
        defaultColors.put("sapphire", "#0F52BA");
        defaultColors.put("amber", "#FFBF00");
        defaultColors.put("charcoal", "#36454F");
        defaultColors.put("ivory", "#FFFFF0");
        defaultColors.put("cream", "#FFFDD0");
        defaultColors.put("coffee", "#6F4E37");
        defaultColors.put("bronze", "#CD7F32");
        defaultColors.put("rose", "#FF007F");
        defaultColors.put("wine", "#722F37");
        defaultColors.put("denim", "#1560BD");
        defaultColors.put("slate", "#708090");
        defaultColors.put("periwinkle", "#CCCCFF");
        defaultColors.put("mauve", "#E0B0FF");
        defaultColors.put("taupe", "#483C32");
        defaultColors.put("ochre", "#CC7722");
        defaultColors.put("cerulean", "#007BA7");
        defaultColors.put("aquamarine", "#7FFFD4");
        defaultColors.put("seafoam", "#93E9BE");
        defaultColors.put("crimson", "#DC143C");
        defaultColors.put("fuchsia", "#FF00FF");
        defaultColors.put("orchid", "#DA70D6");
        defaultColors.put("steel blue", "#4682B4");
        defaultColors.put("midnight blue", "#191970");
        defaultColors.put("forest green", "#228B22");
        defaultColors.put("sea green", "#2E8B57");
        defaultColors.put("lime", "#00FF00");
        defaultColors.put("chartreuse", "#7FFF00");
        defaultColors.put("honeydew", "#F0FFF0");
        defaultColors.put("snow", "#FFFAFA");
        defaultColors.put("ghost white", "#F8F8FF");
        defaultColors.put("floral white", "#FFFAF0");
        // Add more defaults as needed

        return defaultColors.getOrDefault(colorName.toLowerCase(), "#CCCCCC");
    }

    public DealDto mapDealToDto(Deal deal) {
        return DealDto.builder()
                .id(deal.getId())
                .product(mapProductToDtoBasic(deal.getProduct()))
                .startDate(deal.getStartDate())
                .endDate(deal.getEndDate())
                .discountPercentage(deal.getDiscountPercentage())
                .active(deal.isActive())
                .build();
    }

}
