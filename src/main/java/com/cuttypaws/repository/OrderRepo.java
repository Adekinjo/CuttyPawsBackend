package com.cuttypaws.repository;

import com.cuttypaws.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OrderRepo extends JpaRepository<Order, Long> {

}

