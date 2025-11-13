package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Order> findByStatus(String status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.cafeTable.id = :tableId AND o.status = 'PENDING'")
    Optional<Order> findPendingOrderByTableId(@Param("tableId") Long tableId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID' AND o.paidAt >= :startOfDay AND o.paidAt < :endOfDay")
    BigDecimal findTotalRevenueByDateRange(@Param("startOfDay") LocalDateTime startOfDay,
                                           @Param("endOfDay") LocalDateTime endOfDay);

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

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.customer " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithCustomer(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID' AND o.paidAt >= :startDateTime AND o.paidAt < :endDateTime")
    BigDecimal sumAmountBetweenDates(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    List<Order> findByStatusAndPaidAtBetween(String status, LocalDateTime startDateTime, LocalDateTime endDateTime);

    long countByCafeTableId(Long tableId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PAID' AND o.paidAt >= :startDateTime AND o.paidAt < :endDateTime")
    Long countPaidOrdersBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'PAID' AND o.paidAt >= :startDateTime AND o.paidAt < :endDateTime")
    BigDecimal sumPaidRevenueBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT o.id FROM Order o " +
            "WHERE o.customer.id = :customerId " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:startDateTime IS NULL OR COALESCE(o.paidAt, o.createdAt) >= :startDateTime) " +
            "AND (:endDateTime IS NULL OR COALESCE(o.paidAt, o.createdAt) <= :endDateTime)")
    Page<Long> findCustomerOrderIds(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.product " +
            "LEFT JOIN FETCH o.cafeTable " +
            "LEFT JOIN FETCH o.user " +
            "WHERE o.id IN :ids")
    List<Order> findCustomerOrdersByIds(@Param("ids") List<Long> ids);

    @Query("SELECT COUNT(o.id) AS totalOrders, " +
            "COALESCE(SUM(o.totalAmount), 0) AS totalAmount, " +
            "MAX(COALESCE(o.paidAt, o.createdAt)) AS lastPurchaseDate " +
            "FROM Order o " +
            "WHERE o.customer.id = :customerId " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:startDateTime IS NULL OR COALESCE(o.paidAt, o.createdAt) >= :startDateTime) " +
            "AND (:endDateTime IS NULL OR COALESCE(o.paidAt, o.createdAt) <= :endDateTime)")
    CustomerPurchaseAggregate calculateCustomerPurchaseAggregate(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.cafeTable " +
            "WHERE o.status = :status " +
            "AND COALESCE(o.paidAt, o.createdAt) >= :startDateTime " +
            "AND COALESCE(o.paidAt, o.createdAt) < :endDateTime")
    List<Order> findByStatusAndDateRange(
            @Param("status") String status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT o FROM Order o " +
            "WHERE o.user.id = :userId " +
            "AND o.status = 'PAID' " +
            "AND o.paidAt BETWEEN :startDateTime AND :endDateTime")
    List<Order> findPaidOrdersForStaffBetween(
            @Param("userId") Long userId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

}
