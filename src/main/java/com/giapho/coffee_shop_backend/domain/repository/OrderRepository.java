package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Tìm các đơn hàng trong một khoảng thời gian (dùng cho báo cáo)
     */
    Page<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Tìm các đơn hàng theo trạng thái
     */
    Page<Order> findByStatus(String status, Pageable pageable);

    /**
     * Tìm đơn hàng đang 'PENDING' (chưa thanh toán) của 1 bàn
     * (Một bàn chỉ nên có 1 đơn PENDING tại 1 thời điểm)
     */
    @Query("SELECT o FROM Order o WHERE o.cafeTable.id = :tableId AND o.status = 'PENDING'")
    Optional<Order> findPendingOrderByTableId(@Param("tableId") Long tableId);

    @Query("SELECT o FROM Order o WHERE o.cafeTable.id = :tableId AND o.status = 'EMPTY'")
    Optional<Order> findEmptyOrderByTableId(@Param("tableId") Long tableId);
//
//    @Query("SELECT o FROM Order o WHERE o.cafeTable.id = :tableId AND o.status = 'PENDING'")
//    Optional<Order> findPendingOderByTableId(Long id);
}
