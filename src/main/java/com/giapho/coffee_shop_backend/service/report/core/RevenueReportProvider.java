package com.giapho.coffee_shop_backend.service.report.core;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.repository.OrderDetailRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.dto.BestSellerDTO;
import com.giapho.coffee_shop_backend.dto.CategorySalesDTO;
import com.giapho.coffee_shop_backend.dto.HourlySalesDTO;
import com.giapho.coffee_shop_backend.dto.PaymentMethodStatsDTO;
import com.giapho.coffee_shop_backend.dto.ProductSalesSummaryDTO;
import com.giapho.coffee_shop_backend.dto.ProductSalesSummaryResponseDTO;
import com.giapho.coffee_shop_backend.dto.SalesComparisonDTO;
import com.giapho.coffee_shop_backend.service.report.helper.ReportCalculationHelper;
import com.giapho.coffee_shop_backend.service.report.helper.ReportDateValidator;
import com.giapho.coffee_shop_backend.service.report.helper.ReportTimeSeriesHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.math.RoundingMode;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevenueReportProvider {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    public BigDecimal getDailyRevenue(LocalDate date) {
        ReportDateValidator.validateMandatoryRange(date, date);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return ReportCalculationHelper.defaultZero(orderRepository.findTotalRevenueByDateRange(OrderStatus.PAID, startOfDay, endOfDay));
    }

    public Map<String, BigDecimal> getProfitReport(LocalDate startDate, LocalDate endDate) {
        ReportDateValidator.validateMandatoryRange(startDate, endDate);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        BigDecimal totalRevenue = ReportCalculationHelper.defaultZero(orderRepository.sumAmountBetweenDates(OrderStatus.PAID, start, end));
        List<OrderDetail> details = orderDetailRepository.findPaidOrderDetailsBetweenDates(start, end);

        BigDecimal totalCost = details.stream()
                .map(detail -> {
                    BigDecimal productCost = detail.getProduct() != null ? detail.getProduct().getCost() : null;
                    if (productCost == null) {
                        return BigDecimal.ZERO;
                    }
                    return productCost.multiply(BigDecimal.valueOf(detail.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit = totalRevenue.subtract(totalCost);
        return Map.of(
                "totalRevenue", totalRevenue,
                "totalCostOfGoodsSold", totalCost,
                "totalProfit", totalProfit
        );
    }

    public List<BestSellerDTO> getBestSellingProducts(LocalDate startDate, LocalDate endDate, int top, String sortBy) {
        ReportDateValidator.validateMandatoryRange(startDate, endDate);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        Pageable pageable = PageRequest.of(0, top);
        if ("revenue".equalsIgnoreCase(sortBy)) {
            return orderDetailRepository.findBestSellersByRevenueBetweenDates(start, end, pageable);
        }
        return orderDetailRepository.findBestSellersByQuantityBetweenDates(start, end, pageable);
    }

    public ProductSalesSummaryResponseDTO getProductSalesSummary(LocalDate startDate, LocalDate endDate) {
        ReportDateValidator.validateMandatoryRange(startDate, endDate);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<ProductSalesSummaryDTO> summaries = orderDetailRepository.findProductSalesSummaryBetweenDates(start, end);
        long totalQuantity = summaries.stream()
                .map(ProductSalesSummaryDTO::getTotalQuantitySold)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        BigDecimal totalRevenue = summaries.stream()
                .map(ProductSalesSummaryDTO::getTotalRevenueGenerated)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProductSalesSummaryResponseDTO.builder()
                .products(summaries)
                .totalQuantitySold(totalQuantity)
                .totalRevenueGenerated(totalRevenue)
                .build();
    }

    public Map<LocalDate, BigDecimal> getRevenueReportByDateRange(LocalDate startDate, LocalDate endDate) {
        ReportDateValidator.validateMandatoryRange(startDate, endDate);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        Map<LocalDate, BigDecimal> revenueByDate = orderRepository.findByStatusAndPaidAtBetween(OrderStatus.PAID, start, end).stream()
                .filter(order -> order.getPaidAt() != null && order.getTotalAmount() != null)
                .collect(Collectors.groupingBy(
                        order -> order.getPaidAt().toLocalDate(),
                        ReportTimeSeriesHelper::newDateOrderedMap,
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)
                ));

        ReportTimeSeriesHelper.fillMissingDates(revenueByDate, startDate, endDate, BigDecimal.ZERO);
        return revenueByDate;
    }

    public List<CategorySalesDTO> getCategorySales(LocalDate startDate, LocalDate endDate) {
        ReportDateValidator.validateMandatoryRange(startDate, endDate);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        List<CategorySalesDTO> categorySales = orderDetailRepository.findCategorySalesBetweenDates(start, end);
        BigDecimal totalRevenue = categorySales.stream()
                .map(CategorySalesDTO::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            categorySales.forEach(dto -> dto.setRevenuePercentage(
                    ReportCalculationHelper.safeDivide(dto.getTotalRevenue(), totalRevenue)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue()
            ));
        }
        return categorySales;
    }

    public List<HourlySalesDTO> getHourlySales(LocalDate date) {
        ReportDateValidator.validateMandatoryRange(date, date);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository.findByStatusAndPaidAtBetween(OrderStatus.PAID, start, end);
        Map<Integer, List<Order>> orderByHour = orders.stream()
                .filter(order -> order.getPaidAt() != null)
                .collect(Collectors.groupingBy(order -> order.getPaidAt().getHour()));

        return java.util.stream.IntStream.range(0, 24)
                .mapToObj(hour -> {
                    List<Order> hourOrders = orderByHour.getOrDefault(hour, Collections.emptyList());
                    long count = hourOrders.size();
                    BigDecimal total = hourOrders.stream()
                            .map(Order::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal avg = count > 0 ? ReportCalculationHelper.safeDivide(total, BigDecimal.valueOf(count)) : BigDecimal.ZERO;
                    return HourlySalesDTO.builder()
                            .hour(hour)
                            .orderCount(count)
                            .revenue(total)
                            .averageOrderValue(avg.doubleValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<PaymentMethodStatsDTO> getPaymentMethodStats(LocalDate startDate, LocalDate endDate) {
        ReportDateValidator.validateMandatoryRange(startDate, endDate);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository.findByStatusAndPaidAtBetween(OrderStatus.PAID, start, end);
        BigDecimal totalAmount = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return orders.stream()
                .filter(order -> order.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(Order::getPaymentMethod))
                .entrySet()
                .stream()
                .map(entry -> {
                    String method = entry.getKey();
                    List<Order> methodOrders = entry.getValue();
                    long count = methodOrders.size();
                    BigDecimal methodTotal = methodOrders.stream()
                            .map(Order::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                            ? ReportCalculationHelper.safeDivide(methodTotal, totalAmount).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;
                    return PaymentMethodStatsDTO.builder()
                            .paymentMethod(method)
                            .orderCount(count)
                            .totalAmount(methodTotal)
                            .percentage(percentage.doubleValue())
                            .build();
                })
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());
    }

    public SalesComparisonDTO compareSalesPeriods(LocalDate currentStart, LocalDate currentEnd,
                                                  LocalDate previousStart, LocalDate previousEnd) {
        ReportDateValidator.validateMandatoryRange(currentStart, currentEnd);
        ReportDateValidator.validateMandatoryRange(previousStart, previousEnd);

        LocalDateTime currentStartDT = currentStart.atStartOfDay();
        LocalDateTime currentEndDT = currentEnd.plusDays(1).atStartOfDay();
        LocalDateTime previousStartDT = previousStart.atStartOfDay();
        LocalDateTime previousEndDT = previousEnd.plusDays(1).atStartOfDay();

        BigDecimal currentRevenue = ReportCalculationHelper.defaultZero(orderRepository.sumAmountBetweenDates(OrderStatus.PAID, currentStartDT, currentEndDT));
        BigDecimal previousRevenue = ReportCalculationHelper.defaultZero(orderRepository.sumAmountBetweenDates(OrderStatus.PAID, previousStartDT, previousEndDT));

        long currentOrders = orderRepository.findByStatusAndPaidAtBetween(OrderStatus.PAID, currentStartDT, currentEndDT).size();
        long previousOrders = orderRepository.findByStatusAndPaidAtBetween(OrderStatus.PAID, previousStartDT, previousEndDT).size();

        BigDecimal growthAmount = currentRevenue.subtract(previousRevenue);
        BigDecimal growthPercent = previousRevenue.compareTo(BigDecimal.ZERO) > 0
                ? ReportCalculationHelper.safeDivide(growthAmount, previousRevenue).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return SalesComparisonDTO.builder()
                .period(currentStart + " to " + currentEnd)
                .currentRevenue(currentRevenue)
                .previousRevenue(previousRevenue)
                .growthAmount(growthAmount)
                .growthPercentage(growthPercent.doubleValue())
                .currentOrders(currentOrders)
                .previousOrders(previousOrders)
                .build();
    }
}
