package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExpenseDTO {

    private Long id; // Chỉ dùng khi hiển thị

    private Long userId; // ID người tạo (chỉ hiển thị)
    private String username; // Tên người tạo (chỉ hiển thị)

    @NotBlank(message = "Expense category is required")
    private String category; // Loại chi phí

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount; // Số tiền

    private String description; // Mô tả

    @NotNull(message = "Expense date is required")
    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDate expenseDate; // Ngày chi

    private LocalDateTime createdAt; // Chỉ hiển thị
    private LocalDateTime updatedAt; // Chỉ hiển thị
}