package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Expense;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.ExpenseRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.ExpenseDTO;
import com.giapho.coffee_shop_backend.exception.expense.ExpenseNotFoundException;
import com.giapho.coffee_shop_backend.exception.expense.InvalidExpenseDateRangeException;
import com.giapho.coffee_shop_backend.exception.user.UserNotFoundException;
import com.giapho.coffee_shop_backend.mapper.ExpenseMapper;
import com.giapho.coffee_shop_backend.service.ExpenseService;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;

    @Override
    public Page<ExpenseDTO> getAllExpenses(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        validateDateRange(startDate, endDate);

        Page<Expense> expenses;
        if (startDate != null && endDate != null) {
            expenses = expenseRepository.findByExpenseDateBetween(startDate, endDate, pageable);
        } else if (startDate != null) {
            expenses = expenseRepository.findByExpenseDateGreaterThanEqual(startDate, pageable);
        } else if (endDate != null) {
            expenses = expenseRepository.findByExpenseDateLessThanEqual(endDate, pageable);
        } else {
            expenses = expenseRepository.findAll(pageable);
        }

        return expenses.map(expenseMapper::toDto);
    }

    @Override
    public ExpenseDTO getExpenseById(Long id) {
        Expense expense = findExpenseById(id);
        return expenseMapper.toDto(expense);
    }

    @Override
    @Transactional
    public ExpenseDTO createExpense(ExpenseDTO expenseDTO) {
        Expense expense = expenseMapper.toEntity(expenseDTO);
        expense.setUser(resolveCurrentUser());
        normalizeExpense(expense);

        Expense saved = expenseRepository.save(expense);
        log.info("Created expense {} for user {}", saved.getId(), saved.getUser().getUsername());
        return expenseMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ExpenseDTO updateExpense(Long id, ExpenseDTO expenseDTO) {
        Expense expense = findExpenseById(id);
        expenseMapper.updateEntityFromDto(expenseDTO, expense);
        normalizeExpense(expense);

        Expense saved = expenseRepository.save(expense);
        log.info("Updated expense {}", id);
        return expenseMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        Expense expense = findExpenseById(id);
        expenseRepository.delete(expense);
        log.info("Deleted expense {}", id);
    }

    private Expense findExpenseById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    private User resolveCurrentUser() {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new UserNotFoundException("Current user"));
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private void normalizeExpense(Expense expense) {
        if (StringUtils.hasText(expense.getCategory())) {
            expense.setCategory(expense.getCategory().trim());
        }
        if (expense.getDescription() != null) {
            expense.setDescription(expense.getDescription().trim());
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidExpenseDateRangeException(startDate, endDate);
        }
    }
}
