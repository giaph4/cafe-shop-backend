package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.Customer;
import com.giapho.coffee_shop_backend.dto.CustomerAnalyticsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    Page<Customer> findByFullNameContainingIgnoreCaseOrPhoneContaining(String fullName, String phone, Pageable pageable);

    @Query("SELECT new com.giapho.coffee_shop_backend.dto.CustomerAnalyticsDTO(" +
            "  c.id, " +
            "  c.fullName, " +
            "  c.phone, " +
            "  CAST(COUNT(o.id) AS long), " +
            "  CAST(COALESCE(SUM(o.totalAmount), 0) AS java.math.BigDecimal), " +
            "  CAST(COALESCE(AVG(o.totalAmount), 0) AS java.math.BigDecimal), " +
            "  c.loyaltyPoints, " +
            "  '') " +
            "FROM Customer c " +
            "LEFT JOIN Order o ON o.customer.id = c.id AND o.status = 'PAID' AND o.paidAt BETWEEN :startDate AND :endDate " +
            "GROUP BY c.id, c.fullName, c.phone, c.loyaltyPoints " +
            "ORDER BY COALESCE(SUM(o.totalAmount), 0) DESC")
    List<CustomerAnalyticsDTO> findTopCustomersBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}