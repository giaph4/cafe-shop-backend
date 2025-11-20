package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.repository.projection.OrderSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"orderDetails", "orderDetails.product", "cafeTable", "user", "customer"})
    Page<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @EntityGraph(attributePaths = {"orderDetails", "orderDetails.product", "cafeTable", "user", "customer"})
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT o.id AS id, t.name AS tableName, u.username AS staffUsername, o.type AS type, o.status AS status, " +
            "o.subTotal AS subTotal, o.totalAmount AS totalAmount, o.createdAt AS createdAt, o.paidAt AS paidAt " +
            "FROM Order o " +
            "LEFT JOIN o.cafeTable t " +
            "LEFT JOIN o.user u")
    Page<OrderSummaryProjection> findOrderSummaries(Pageable pageable);

    @Query("SELECT o.id AS id, t.name AS tableName, u.username AS staffUsername, o.type AS type, o.status AS status, " +
            "o.subTotal AS subTotal, o.totalAmount AS totalAmount, o.createdAt AS createdAt, o.paidAt AS paidAt " +
            "FROM Order o " +
            "LEFT JOIN o.cafeTable t " +
            "LEFT JOIN o.user u " +
            "WHERE o.status = :status")
    Page<OrderSummaryProjection> findOrderSummariesByStatus(@Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT o.id AS id, t.name AS tableName, u.username AS staffUsername, o.type AS type, o.status AS status, " +
            "o.subTotal AS subTotal, o.totalAmount AS totalAmount, o.createdAt AS createdAt, o.paidAt AS paidAt " +
            "FROM Order o " +
            "LEFT JOIN o.cafeTable t " +
            "LEFT JOIN o.user u " +
            "WHERE o.createdAt >= :startDateTime AND o.createdAt < :endDateTime")
    Page<OrderSummaryProjection> findOrderSummariesByCreatedAtBetween(@Param("startDateTime") LocalDateTime startDateTime,
                                                                     @Param("endDateTime") LocalDateTime endDateTime,
                                                                     Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.cafeTable.id = :tableId AND o.status = :status")
    Optional<Order> findByTableIdAndStatus(@Param("tableId") Long tableId, @Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.paidAt >= :startOfDay AND o.paidAt < :endOfDay")
    BigDecimal findTotalRevenueByDateRange(@Param("status") OrderStatus status,
                                           @Param("startOfDay") LocalDateTime startOfDay,
                                           @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.product " +
            "LEFT JOIN FETCH o.cafeTable " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.customer " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.product " +
            "LEFT JOIN FETCH o.cafeTable " +
            "LEFT JOIN FETCH o.user " +
            "LEFT JOIN FETCH o.customer " +
            "WHERE o.id = :id AND o.status = :status")
    Optional<Order> findByIdAndStatusWithDetails(@Param("id") Long id, @Param("status") OrderStatus status);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.customer " +
            "LEFT JOIN FETCH o.cafeTable " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithCustomer(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.paidAt >= :startDateTime AND o.paidAt < :endDateTime")
    BigDecimal sumAmountBetweenDates(
            @Param("status") OrderStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    List<Order> findByStatusAndPaidAtBetween(OrderStatus status, LocalDateTime startDateTime, LocalDateTime endDateTime);

    long countByCafeTableId(Long tableId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.paidAt >= :startDateTime AND o.paidAt < :endDateTime")
    Long countPaidOrdersBetween(
            @Param("status") OrderStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status AND o.paidAt >= :startDateTime AND o.paidAt < :endDateTime")
    BigDecimal sumPaidRevenueBetween(
            @Param("status") OrderStatus status,
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
            @Param("status") OrderStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT o FROM Order o " +
            "WHERE o.user.id = :userId " +
            "AND o.status = :status " +
            "AND o.paidAt BETWEEN :startDateTime AND :endDateTime")
    List<Order> findOrdersForStaffBetween(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    List<Order> findByShiftSessionId(Long shiftSessionId);

    @Query("SELECT o FROM Order o WHERE o.shiftSession.id = :sessionId AND o.status = :status")
    Optional<Order> findByShiftSessionIdAndStatus(
            @Param("sessionId") Long shiftSessionId,
            @Param("status") OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.product " +
            "LEFT JOIN FETCH o.user " +
            "WHERE o.shiftSession.id = :sessionId")
    List<Order> findByShiftSessionIdWithDetails(@Param("sessionId") Long sessionId);
}
