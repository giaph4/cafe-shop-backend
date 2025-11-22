package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.dto.BestSellerDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    @Query("SELECT od FROM OrderDetail od JOIN FETCH od.product p JOIN od.order o " +
            "WHERE o.status = 'PAID' AND o.paidAt BETWEEN :startDate AND :endDate")
    List<OrderDetail> findPaidOrderDetailsBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT new com.giapho.coffee_shop_backend.dto.BestSellerDTO(" +
            "  od.product.id, " +
            "  od.product.name, " +
            "  SUM(od.quantity) as totalQuantity, " +
            "  SUM(od.quantity * od.priceAtOrder) as totalRevenue) " + // Đã sửa
            "FROM OrderDetail od JOIN od.order o " +
            "WHERE o.status = 'PAID' AND o.paidAt BETWEEN :startDate AND :endDate " +
            "GROUP BY od.product.id, od.product.name " +
            "ORDER BY totalQuantity DESC")
    List<BestSellerDTO> findBestSellersByQuantityBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT new com.giapho.coffee_shop_backend.dto.BestSellerDTO(" +
            "  od.product.id, " +
            "  od.product.name, " +
            "  SUM(od.quantity) as totalQuantity, " +
            "  SUM(od.quantity * od.priceAtOrder) as totalRevenue) " + // Đã sửa
            "FROM OrderDetail od JOIN od.order o " +
            "WHERE o.status = 'PAID' AND o.paidAt BETWEEN :startDate AND :endDate " +
            "GROUP BY od.product.id, od.product.name " +
            "ORDER BY totalRevenue DESC")
    List<BestSellerDTO> findBestSellersByRevenueBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT new com.giapho.coffee_shop_backend.dto.ProductSalesSummaryDTO(" +
            "  od.product.id, " +
            "  od.product.name, " +
            "  CAST(SUM(od.quantity) AS long), " +
            "  CAST(SUM(od.quantity * od.priceAtOrder) AS java.math.BigDecimal)) " +
            "FROM OrderDetail od JOIN od.order o " +
            "WHERE o.status = 'PAID' AND o.paidAt BETWEEN :startDate AND :endDate " +
            "GROUP BY od.product.id, od.product.name " +
            "ORDER BY od.product.name ASC")
    List<com.giapho.coffee_shop_backend.dto.ProductSalesSummaryDTO> findProductSalesSummaryBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByProductId(Long productId);

    @Query("SELECT new com.giapho.coffee_shop_backend.dto.CategorySalesDTO(" +
            "  c.id, " +
            "  c.name, " +
            "  CAST(SUM(od.quantity) AS long), " +
            "  CAST(SUM(od.quantity * od.priceAtOrder) AS java.math.BigDecimal), " +
            "  0.0, " +
            "  CAST(COUNT(DISTINCT p.id) AS int)) " +
            "FROM OrderDetail od " +
            "JOIN od.product p " +
            "JOIN p.category c " +
            "JOIN od.order o " +
            "WHERE o.status = 'PAID' AND o.paidAt BETWEEN :startDate AND :endDate " +
            "GROUP BY c.id, c.name " +
            "ORDER BY SUM(od.quantity * od.priceAtOrder) DESC")
    List<com.giapho.coffee_shop_backend.dto.CategorySalesDTO> findCategorySalesBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}