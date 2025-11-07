package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Order> findByStatus(String status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.cafeTable.id = :tableId AND o.status = 'PENDING'")
    Optional<Order> findPendingOrderByTableId(@Param("tableId") Long tableId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID' AND FUNCTION('DATE', o.paidAt) = :date")
    BigDecimal findTotalRevenueByDate(@Param("date") LocalDate date); // Nhận vào LocalDate

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderDetails " +
            "LEFT JOIN FETCH o.cafeTable " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.customer " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderDetails " +
            "LEFT JOIN FETCH o.cafeTable " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.customer " +
            "WHERE o.id = :id AND o.status = 'PENDING'")
    Optional<Order> findPendingOrderByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID' AND o.paidAt >= :startDateTime AND o.paidAt < :endDateTime")
    BigDecimal sumAmountBetweenDates(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    List<Order> findByStatusAndPaidAtBetween(String status, LocalDateTime startDateTime, LocalDateTime endDateTime);

    long countByCafeTableId(Long tableId);

    @Query("SELECT o FROM Order o WHERE o.status = 'PAID' AND FUNCTION('DATE', o.paidAt) = CURRENT_DATE")
    List<Order> findTodayPaidOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PAID' AND FUNCTION('DATE', o.paidAt) = CURRENT_DATE")
    Long countTodayOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PAID' AND FUNCTION('YEAR', o.paidAt) = FUNCTION('YEAR', CURRENT_DATE) AND FUNCTION('MONTH', o.paidAt) = FUNCTION('MONTH', CURRENT_DATE)")
    Long countMonthOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PAID' AND FUNCTION('YEAR', o.paidAt) = FUNCTION('YEAR', CURRENT_DATE)")
    Long countYearOrders();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID' AND FUNCTION('YEAR', o.paidAt) = FUNCTION('YEAR', CURRENT_DATE) AND FUNCTION('MONTH', o.paidAt) = FUNCTION('MONTH', CURRENT_DATE)")
    BigDecimal sumMonthRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID' AND FUNCTION('YEAR', o.paidAt) = FUNCTION('YEAR', CURRENT_DATE)")
    BigDecimal sumYearRevenue();

}
