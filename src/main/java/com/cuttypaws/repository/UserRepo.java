package com.cuttypaws.repository;

import com.cuttypaws.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findById(Long userId);
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address WHERE u.id = :id")
    Optional<User> findByIdWithAddress(@Param("id") Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address")
    List<User> findAllWithAddress();

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.address " +
            "LEFT JOIN FETCH u.orderItemsList o " +
            "LEFT JOIN FETCH o.product " +
            "WHERE u.id = :id")
    Optional<User> findByIdWithAddressAndOrders(@Param("id") Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.address LEFT JOIN FETCH u.orderItemsList")
    List<User> findAllWithAddressAndOrders();

    @Query("SELECT u FROM User u WHERE u.userRole = com.cuttypaws.enums.UserRole.ROLE_COMPANY")
    List<User> findAllByRoleCompany();

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.products WHERE u.id = :id AND u.userRole = com.cuttypaws.enums.UserRole.ROLE_COMPANY")
    Optional<User> findCompanyWithProductsById(@Param("id") Long id);

}

