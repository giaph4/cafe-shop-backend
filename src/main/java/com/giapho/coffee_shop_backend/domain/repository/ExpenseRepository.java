package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Tìm chi phí theo loại (phân trang)
     */
    Page<Expense> findByCategoryIgnoreCase(String category, Pageable pageable);

    /**
     * Tìm chi phí trong một khoảng ngày (phân trang)
     */
    Page<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Tìm chi phí theo người tạo (phân trang)
     */
    Page<Expense> findByUserId(Long userId, Pageable pageable);

    /**
     * Tính tổng chi phí trong một khoảng ngày theo loại chi phí
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.category = :category AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByCategoryAndDateBetween(
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Tính tổng chi phí trong một khoảng ngày (tất cả các loại)
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}