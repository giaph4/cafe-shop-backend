package com.giapho.coffee_shop_backend.service.report.core;

import com.giapho.coffee_shop_backend.domain.entity.Expense;
import com.giapho.coffee_shop_backend.domain.repository.ExpenseRepository;
import com.giapho.coffee_shop_backend.service.report.helper.ReportCalculationHelper;
import com.giapho.coffee_shop_backend.service.report.helper.ReportDateValidator;
import com.giapho.coffee_shop_backend.service.report.helper.ReportTimeSeriesHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseReportProvider {

    private final ExpenseRepository expenseRepository;

    public Map<LocalDate, Map<String, BigDecimal>> getExpenseReportByDateRange(LocalDate startDate, LocalDate endDate) {
        ReportDateValidator.validateMandatoryRange(startDate, endDate);
        List<Expense> expenses = expenseRepository.findByExpenseDateBetween(startDate, endDate, Pageable.unpaged()).getContent();

        Map<LocalDate, Map<String, BigDecimal>> dailyExpenses = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getExpenseDate,
                        ReportTimeSeriesHelper::newDateOrderedMap,
                        Collectors.groupingBy(
                                Expense::getCategory,
                                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                        )
                ));

        ReportTimeSeriesHelper.fillMissingDates(dailyExpenses, startDate, endDate, Map.of());
        return dailyExpenses;
    }

    public BigDecimal getTotalExpenses(LocalDate startDate, LocalDate endDate) {
        ReportDateValidator.validateOptionalRange(startDate, endDate);
        return ReportCalculationHelper.defaultZero(expenseRepository.sumAmountByOptionalDateRange(startDate, endDate));
    }
}
