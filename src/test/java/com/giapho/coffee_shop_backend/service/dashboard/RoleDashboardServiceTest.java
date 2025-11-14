package com.giapho.coffee_shop_backend.service.dashboard;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.PayrollCycle;
import com.giapho.coffee_shop_backend.domain.entity.PayrollSummary;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.ShiftTemplate;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.domain.repository.AttendanceRecordRepository;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.PayrollSummaryRepository;
import com.giapho.coffee_shop_backend.domain.repository.PurchaseOrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftInstanceRepository;
import com.giapho.coffee_shop_backend.domain.repository.SupplierRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.analytics.DashboardMetricsDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.AdminDashboardDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.ManagerDashboardDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.StaffDashboardDTO;
import com.giapho.coffee_shop_backend.service.DashboardAnalyticsService;
import com.giapho.coffee_shop_backend.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleDashboardServiceTest {

    @Mock
    private ReportService reportService;
    @Mock
    private DashboardAnalyticsService dashboardAnalyticsService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock
    private ShiftInstanceRepository shiftInstanceRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private PayrollSummaryRepository payrollSummaryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleDashboardService roleDashboardService;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
    }

    @Test
    void buildAdminDashboard_shouldAggregateMetrics() {
        when(reportService.getDailyRevenue(any())).thenReturn(BigDecimal.valueOf(120));
        when(orderRepository.sumPaidRevenueBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(2400), BigDecimal.valueOf(28000));
        when(orderRepository.countPaidOrdersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(20L, 20L, 400L, 4200L);
        when(orderRepository.findByStatusAndDateRange(anyString(), any(), any()))
                .thenReturn(List.of(TestDataFactory.order(1L)));
        Map<String, BigDecimal> profitMap = Map.of(
                "totalProfit", BigDecimal.valueOf(150),
                "totalRevenue", BigDecimal.valueOf(300)
        );
        when(reportService.getProfitReport(any(), any())).thenReturn(profitMap);

        when(ingredientRepository.findIngredientsBelowReorderLevel()).thenReturn(List.of(
                TestDataFactory.ingredient(1L, "Sữa tươi", BigDecimal.valueOf(5), BigDecimal.valueOf(10))
        ));
        when(supplierRepository.count()).thenReturn(5L);
        when(purchaseOrderRepository.findByStatus(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(TestDataFactory.purchaseOrder(1L, today.minusDays(1)))));

        DashboardMetricsDTO metrics = DashboardMetricsDTO.builder()
                .from(today.minusDays(30))
                .to(today)
                .totalOrders(60)
                .paidOrders(55)
                .cancelledOrders(5)
                .totalRevenue(BigDecimal.valueOf(6000))
                .averageOrderValue(BigDecimal.valueOf(109))
                .totalDiscount(BigDecimal.TEN)
                .voucherUsageCount(4L)
                .topProducts(List.of(
                        DashboardMetricsDTO.BestSellerMetric.builder()
                                .productId(1L)
                                .productName("Latte")
                                .totalQuantity(200)
                                .totalRevenue(BigDecimal.valueOf(2000))
                                .build()
                ))
                .topCustomers(List.of(
                        DashboardMetricsDTO.CustomerMetric.builder()
                                .customerId(1L)
                                .customerName("Nguyễn Văn A")
                                .phone("0900000000")
                                .orderCount(20)
                                .totalSpend(BigDecimal.valueOf(1500))
                                .averageSpend(BigDecimal.valueOf(75))
                                .build()
                ))
                .topStaff(List.of(
                        DashboardMetricsDTO.StaffMetric.builder()
                                .staffId(10L)
                                .staffName("Lê Thu B")
                                .orderCount(120)
                                .totalRevenue(BigDecimal.valueOf(3200))
                                .build()
                ))
                .build();
        when(dashboardAnalyticsService.collectMetrics(any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(metrics);

        AdminDashboardDTO result = roleDashboardService.buildAdminDashboard();

        assertEquals(BigDecimal.valueOf(120), result.revenue().today());
        assertEquals(20L, result.orders().today());
        assertEquals(1, result.topProducts().size());
        assertEquals(1, result.topCustomers().size());
        assertEquals(1, result.topStaff().size());
        assertFalse(result.alerts().isEmpty());
    }

    @Test
    void buildManagerDashboard_shouldCombineShiftInventoryPayroll() {
        ShiftTemplate template = ShiftTemplate.builder()
                .requiredRoles(Set.of("BARISTA", "CASHIER"))
                .build();
        ShiftInstance instance = ShiftInstance.builder()
                .id(101L)
                .shiftDate(today)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .status(ShiftStatus.PLANNED)
                .template(template)
                .assignments(Set.of())
                .build();
        when(shiftInstanceRepository.findWithTemplateAndAssignmentsBetween(any(), any())).thenReturn(List.of(instance));
        when(shiftAssignmentRepository.findByStatus(ShiftAssignmentStatus.COMPLETED))
                .thenReturn(List.of(TestDataFactory.assignment(201L, ShiftAssignmentStatus.COMPLETED, today.minusDays(1))));
        when(shiftAssignmentRepository.findByStatus(ShiftAssignmentStatus.IN_PROGRESS))
                .thenReturn(List.of(TestDataFactory.assignment(202L, ShiftAssignmentStatus.IN_PROGRESS, today)));
        when(shiftAssignmentRepository.findByStatus(ShiftAssignmentStatus.CANCELLED))
                .thenReturn(List.of(TestDataFactory.assignment(203L, ShiftAssignmentStatus.CANCELLED, today.minusDays(2))));
        when(ingredientRepository.findIngredientsBelowReorderLevel())
                .thenReturn(List.of(TestDataFactory.ingredient(2L, "Cà phê hạt", BigDecimal.valueOf(3), BigDecimal.valueOf(8))));
        when(purchaseOrderRepository.findByStatus(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(TestDataFactory.purchaseOrder(2L, today.minusDays(2)))));

        PayrollCycle cycle = PayrollCycle.builder()
                .id(1L)
                .code("P-2025-11")
                .startDate(today.minusDays(14))
                .endDate(today)
                .build();
        PayrollSummary summary = PayrollSummary.builder()
                .cycle(cycle)
                .user(TestDataFactory.user(301L, "manager"))
                .totalNetPayroll(BigDecimal.valueOf(750))
                .totalBonus(BigDecimal.valueOf(120))
                .totalPenalty(BigDecimal.valueOf(30))
                .totalAdjustment(BigDecimal.valueOf(90))
                .build();
        when(payrollSummaryRepository.search(null, null)).thenReturn(List.of(summary));

        when(shiftAssignmentRepository.findByShift_ShiftDateBetween(any(), any()))
                .thenReturn(List.of(TestDataFactory.assignment(204L, ShiftAssignmentStatus.SCHEDULED, today)));
        when(orderRepository.findByStatusAndDateRange(anyString(), any(), any()))
                .thenReturn(List.of(TestDataFactory.order(1L)));

        DashboardMetricsDTO metrics = DashboardMetricsDTO.builder()
                .from(today.minusDays(7))
                .to(today)
                .totalOrders(80)
                .paidOrders(70)
                .cancelledOrders(10)
                .totalRevenue(BigDecimal.valueOf(7000))
                .averageOrderValue(BigDecimal.valueOf(100))
                .totalDiscount(BigDecimal.valueOf(50))
                .voucherUsageCount(5L)
                .topProducts(List.of())
                .topCustomers(List.of())
                .topStaff(List.of(
                        DashboardMetricsDTO.StaffMetric.builder()
                                .staffId(401L)
                                .staffName("Trần Văn C")
                                .orderCount(45)
                                .totalRevenue(BigDecimal.valueOf(2500))
                                .build()
                ))
                .build();
        when(dashboardAnalyticsService.collectMetrics(any(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(metrics);

        ManagerDashboardDTO managerDashboard = roleDashboardService.buildManagerDashboard();

        assertNotNull(managerDashboard.shiftOverview());
        assertFalse(managerDashboard.inventory().alerts().isEmpty());
        assertEquals(750, managerDashboard.payroll().estimatedPayroll().intValue());
        assertEquals(1, managerDashboard.pendingApprovals().size());
    }

    @Test
    void buildStaffDashboard_shouldSummarizePersonalMetrics() {
        long staffId = 999L;
        ShiftAssignment assignment = TestDataFactory.assignment(501L, ShiftAssignmentStatus.COMPLETED, today.minusDays(1));
        when(shiftAssignmentRepository.findByUserIdAndShift_ShiftDateBetween(anyLong(), any(), any()))
                .thenReturn(List.of(assignment));

        AttendanceRecord attendanceRecord = TestDataFactory.attendanceRecord(
                LocalDateTime.of(today.minusDays(1), LocalTime.of(8, 5)),
                LocalDateTime.of(today.minusDays(1), LocalTime.of(12, 0)),
                5,
                0
        );
        when(attendanceRecordRepository.findByAssignmentId(anyLong()))
                .thenReturn(List.of(attendanceRecord));

        when(orderRepository.findPaidOrdersForStaffBetween(anyLong(), any(), any()))
                .thenReturn(List.of(TestDataFactory.order(1001L)));

        PayrollSummary summary = PayrollSummary.builder()
                .cycle(PayrollCycle.builder().id(2L).startDate(today.minusDays(15)).endDate(today).build())
                .user(TestDataFactory.user(staffId, "staff"))
                .totalNetPayroll(BigDecimal.valueOf(550))
                .totalBonus(BigDecimal.valueOf(80))
                .totalPenalty(BigDecimal.valueOf(10))
                .totalAdjustment(BigDecimal.valueOf(70))
                .build();
        when(payrollSummaryRepository.search(null, staffId)).thenReturn(List.of(summary));

        StaffDashboardDTO staffDashboard = roleDashboardService.buildStaffDashboard(staffId);

        assertEquals(1, staffDashboard.shiftSummary().shiftsThisWeek());
        assertFalse(staffDashboard.upcomingShifts().isEmpty());
        assertEquals(BigDecimal.valueOf(550), staffDashboard.payroll().estimatedCurrentCycle());
    }

    private static class TestDataFactory {

        private static Ingredient ingredient(Long id, String name, BigDecimal onHand, BigDecimal reorder) {
            return Ingredient.builder()
                    .id(id)
                    .name(name)
                    .quantityOnHand(onHand)
                    .reorderLevel(reorder)
                    .build();
        }

        private static PurchaseOrder purchaseOrder(Long id, LocalDate orderDate) {
            return PurchaseOrder.builder()
                    .id(id)
                    .orderDate(orderDate.atStartOfDay())
                    .status("PENDING")
                    .build();
        }

        private static ShiftAssignment assignment(Long id, ShiftAssignmentStatus status, LocalDate date) {
            ShiftInstance instance = ShiftInstance.builder()
                    .id(id + 100)
                    .shiftDate(date)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(12, 0))
                    .status(ShiftStatus.PLANNED)
                    .build();
            User user = user(id, "user" + id);
            return ShiftAssignment.builder()
                    .id(id)
                    .shift(instance)
                    .user(user)
                    .status(status)
                    .plannedStart(LocalTime.of(8, 0))
                    .plannedEnd(LocalTime.of(12, 0))
                    .plannedMinutes(240)
                    .build();
        }

        private static AttendanceRecord attendanceRecord(LocalDateTime checkIn, LocalDateTime checkOut, int late, int early) {
            return AttendanceRecord.builder()
                    .checkInAt(checkIn)
                    .checkOutAt(checkOut)
                    .lateMinutes(late)
                    .earlyLeaveMinutes(early)
                    .build();
        }

        private static com.giapho.coffee_shop_backend.domain.entity.Order order(Long id) {
            com.giapho.coffee_shop_backend.domain.entity.Order order = new com.giapho.coffee_shop_backend.domain.entity.Order();
            order.setId(id);
            order.setTotalAmount(BigDecimal.valueOf(120));
            order.setCreatedAt(LocalDateTime.now().minusDays(1));
            return order;
        }

        private static User user(Long id, String username) {
            return User.builder()
                    .id(id)
                    .username(username)
                    .fullName(username.toUpperCase())
                    .status("ACTIVE")
                    .build();
        }
    }
}
