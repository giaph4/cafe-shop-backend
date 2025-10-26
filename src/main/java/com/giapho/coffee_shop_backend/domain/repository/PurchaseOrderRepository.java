package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    // Tìm phiếu nhập theo trạng thái (có phân trang)
    Page<PurchaseOrder> findByStatus(String status, Pageable pageable);

    // Tìm phiếu nhập theo nhà cung cấp (có phân trang)
    Page<PurchaseOrder> findBySupplierId(Long supplierId, Pageable pageable);

    // Tìm phiếu nhập trong khoảng thời gian (có phân trang)
    Page<PurchaseOrder> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}