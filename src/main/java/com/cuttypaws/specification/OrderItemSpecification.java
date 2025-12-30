package com.cuttypaws.specification;

import com.cuttypaws.entity.OrderItem;
import com.cuttypaws.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class OrderItemSpecification {

    // Specification to filter order item by status
    public static Specification<OrderItem> hasStatus(OrderStatus orderStatus){
        return ((root, query, criteriaBuilder)
                -> orderStatus != null ? criteriaBuilder.equal(root.get("orderStatus"), orderStatus) : null);
    }

    //  Specification to filter order item by date range
    public static Specification<OrderItem> createdBetween(LocalDateTime startDate, LocalDateTime endDate){
        return((root, query, criteriaBuilder) -> {
            if(startDate != null && endDate != null){
                return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
            }else if(startDate != null){
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            }else if(endDate != null){
                return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }else{
                return null;
            }
        });
    }

    //  Generate specification to filter order item by item id
    public static Specification<OrderItem> hasItemId(Long itemId){
        return ((root, query, criteriaBuilder) ->
                itemId != null ? criteriaBuilder.equal(root.get("id"), itemId) : null);
    }
}
