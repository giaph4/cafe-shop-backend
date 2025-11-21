package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.ExpenseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ExpenseService {

    Page<ExpenseDTO> getAllExpenses(LocalDate startDate, LocalDate endDate, Pageable pageable);

    ExpenseDTO getExpenseById(Long id);

    ExpenseDTO createExpense(ExpenseDTO expenseDTO);

    ExpenseDTO updateExpense(Long id, ExpenseDTO expenseDTO);

    void deleteExpense(Long id);
}