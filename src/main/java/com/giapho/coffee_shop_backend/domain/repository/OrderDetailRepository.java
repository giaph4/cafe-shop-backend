package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.dto.BestSellerDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    /**
     * Lấy tất cả OrderDetail của các đơn hàng PAID trong khoảng thời gian.
     * Join Fetch Product để lấy thông tin cost.
     */
    @Query("SELECT od FROM OrderDetail od JOIN FETCH od.product p JOIN od.order o " +
            "WHERE o.status = 'PAID' AND o.paidAt BETWEEN :startDate AND :endDate")
    List<OrderDetail> findPaidOrderDetailsBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy danh sách sản phẩm bán chạy nhất (theo số lượng) trong khoảng thời gian.
     * Trả về danh sách BestSellerDTO.
     */
    @Query("SELECT new com.giapho.coffee_shop_backend.dto.BestSellerDTO(" +
            "  od.product.id, " +
            "  od.product.name, " +
            "  SUM(od.quantity) as totalQuantity, " +
            "  SUM(od.priceAtOrder * od.quantity) as totalRevenue) " +
            "FROM OrderDetail od JOIN od.order o " +
            "WHERE o.status = 'PAID' AND o.paidAt BETWEEN :startDate AND :endDate " +
            "GROUP BY od.product.id, od.product.name " +
            "ORDER BY totalQuantity DESC") // Sắp xếp theo số lượng giảm dần
    List<BestSellerDTO> findBestSellersByQuantityBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable); // Dùng Pageable để giới hạn số lượng kết quả

    /**
     * Lấy danh sách sản phẩm bán chạy nhất (theo doanh thu) trong khoảng thời gian.
     * Trả về danh sách BestSellerDTO.
     */
    @Query("SELECT new com.giapho.coffee_shop_backend.dto.BestSellerDTO(" +
            "  od.product.id, " +
            "  od.product.name, " +
            "  SUM(od.quantity) as totalQuantity, " +
            "  SUM(od.priceAtOrder * od.quantity) as totalRevenue) " +
            "FROM OrderDetail od JOIN od.order o " +
            "WHERE o.status = 'PAID' AND o.paidAt BETWEEN :startDate AND :endDate " +
            "GROUP BY od.product.id, od.product.name " +
            "ORDER BY totalRevenue DESC") // Sắp xếp theo doanh thu giảm dần
    List<BestSellerDTO> findBestSellersByRevenueBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    long countByProductId(Long productId);
}
