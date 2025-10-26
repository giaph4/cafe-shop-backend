package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Expense;
import com.giapho.coffee_shop_backend.domain.entity.User; // Import User
import com.giapho.coffee_shop_backend.domain.repository.ExpenseRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository; // Import UserRepository
import com.giapho.coffee_shop_backend.dto.ExpenseDTO;
import com.giapho.coffee_shop_backend.mapper.ExpenseMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder; // Import SecurityContextHolder
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate; // Import LocalDate

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository; // Inject UserRepository
    private final ExpenseMapper expenseMapper;

    /**
     * Lấy danh sách chi phí (phân trang, lọc theo ngày)
     */
    @Transactional(readOnly = true)
    public Page<ExpenseDTO> getAllExpenses(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<Expense> expensePage;
        // Nếu không có ngày bắt đầu/kết thúc, lấy tất cả
        if (startDate == null || endDate == null) {
            expensePage = expenseRepository.findAll(pageable);
        } else {
            // Lấy theo khoảng ngày
            expensePage = expenseRepository.findByExpenseDateBetween(startDate, endDate, pageable);
        }
        return expensePage.map(expenseMapper::toDto);
    }

    /**
     * Lấy chi tiết một khoản chi
     */
    @Transactional(readOnly = true)
    public ExpenseDTO getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with id: " + id));
        return expenseMapper.toDto(expense);
    }

    /**
     * Tạo (ghi nhận) một khoản chi mới
     */
    @Transactional
    public ExpenseDTO createExpense(ExpenseDTO expenseDTO) {
        // Lấy thông tin User đang đăng nhập
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        Expense newExpense = expenseMapper.toEntity(expenseDTO);
        newExpense.setUser(currentUser); // Gán người tạo

        Expense savedExpense = expenseRepository.save(newExpense);
        return expenseMapper.toDto(savedExpense);
    }

    /**
     * Cập nhật thông tin một khoản chi
     */
    @Transactional
    public ExpenseDTO updateExpense(Long id, ExpenseDTO expenseDTO) {
        Expense existingExpense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with id: " + id));

        // (Tùy chọn: Kiểm tra xem người cập nhật có phải là người tạo hoặc admin không)
        // String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        // if (!existingExpense.getUser().getUsername().equals(currentUsername) /* && !isAdmin */) {
        //     throw new AccessDeniedException("You don't have permission to update this expense.");
        // }

        // Dùng mapper cập nhật (mapper đã ignore user, createdAt, updatedAt)
        expenseMapper.updateEntityFromDto(expenseDTO, existingExpense);
        Expense updatedExpense = expenseRepository.save(existingExpense);
        return expenseMapper.toDto(updatedExpense);
    }

    /**
     * Xoá một khoản chi
     */
    @Transactional
    public void deleteExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with id: " + id));

        // (Tùy chọn: Kiểm tra quyền xóa)

        expenseRepository.delete(expense);
    }
}