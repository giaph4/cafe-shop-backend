package com.giapho.coffee_shop_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giapho.coffee_shop_backend.domain.enums.PayrollCycleStatus;
import com.giapho.coffee_shop_backend.dto.shift.PayrollCycleRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.PayrollCycleResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.PayrollSummaryDTO;
import com.giapho.coffee_shop_backend.security.JwtService;
import com.giapho.coffee_shop_backend.service.PayrollService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.web.SecurityFilterChain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PayrollController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@Import({PayrollControllerTest.TestConfig.class, PayrollControllerTest.SecurityTestConfig.class})
class PayrollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PayrollService payrollService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        PayrollService payrollService() {
            return mock(PayrollService.class);
        }
    }

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Test
    @DisplayName("Manager có thể lấy danh sách payroll cycles")
    @WithMockUser(roles = "MANAGER")
    void listCycles_ShouldReturnOk() throws Exception {
        PayrollCycleResponseDTO cycle = PayrollCycleResponseDTO.builder()
                .id(10L)
                .code("JAN_2025")
                .name("January 2025")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 31))
                .status(PayrollCycleStatus.DRAFT)
                .createdBy("manager01")
                .updatedBy("manager01")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(payrollService.searchCycles(null, null, null)).thenReturn(List.of(cycle));

        mockMvc.perform(get("/api/v1/shifts/payroll/cycles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("JAN_2025"));
    }

    @Test
    @DisplayName("STAFF bị chặn truy cập danh sách payroll cycles")
    @WithMockUser(roles = "STAFF")
    void listCycles_ShouldReturnForbidden_ForStaff() throws Exception {
        mockMvc.perform(get("/api/v1/shifts/payroll/cycles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tạo chu kỳ payroll thành công")
    @WithMockUser(roles = "MANAGER")
    void createCycle_ShouldReturnCreated() throws Exception {
        PayrollCycleRequestDTO requestDTO = new PayrollCycleRequestDTO(null, "JAN_2025", "January 2025",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), PayrollCycleStatus.DRAFT, "Ghi chú");

        PayrollCycleResponseDTO responseDTO = PayrollCycleResponseDTO.builder()
                .id(10L)
                .code("JAN_2025")
                .name("January 2025")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 31))
                .status(PayrollCycleStatus.DRAFT)
                .createdBy("manager01")
                .updatedBy("manager01")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(payrollService.createCycle(any(PayrollCycleRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/shifts/payroll/cycles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("JAN_2025"));
    }

    @Test
    @DisplayName("Regenerate summaries trả về dữ liệu")
    @WithMockUser(roles = "MANAGER")
    void regenerateSummaries_ShouldReturnData() throws Exception {
        PayrollSummaryDTO summaryDTO = PayrollSummaryDTO.builder()
                .cycleId(20L)
                .cycleCode("JAN_2025")
                .cycleName("January 2025")
                .cycleStartDate(LocalDate.of(2025, 1, 1))
                .cycleEndDate(LocalDate.of(2025, 1, 31))
                .userId(5L)
                .username("staff01")
                .fullName("Nguyễn A")
                .assignmentCount(2)
                .attendanceCount(3)
                .totalActualMinutes(360)
                .totalOrders(15)
                .totalRevenue(new BigDecimal("1500000"))
                .totalBasePayroll(new BigDecimal("700000"))
                .totalBonus(new BigDecimal("30000"))
                .totalPenalty(new BigDecimal("5000"))
                .totalAdjustment(new BigDecimal("25000"))
                .totalNetPayroll(new BigDecimal("725000"))
                .build();

        when(payrollService.regenerateSummaries(20L)).thenReturn(List.of(summaryDTO));

        mockMvc.perform(post("/api/v1/shifts/payroll/cycles/20/regenerate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(5L))
                .andExpect(jsonPath("$[0].totalNetPayroll").value(725000));
    }

    @Test
    @DisplayName("Cập nhật chu kỳ payroll")
    @WithMockUser(roles = "ADMIN")
    void updateCycle_ShouldReturnOk() throws Exception {
        PayrollCycleRequestDTO requestDTO = new PayrollCycleRequestDTO(11L, "FEB_2025", "February 2025",
                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28), PayrollCycleStatus.IN_PROGRESS, null);

        PayrollCycleResponseDTO responseDTO = PayrollCycleResponseDTO.builder()
                .id(11L)
                .code("FEB_2025")
                .name("February 2025")
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 2, 28))
                .status(PayrollCycleStatus.IN_PROGRESS)
                .createdBy("manager01")
                .updatedBy("admin01")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(payrollService.updateCycle(eq(11L), any(PayrollCycleRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/shifts/payroll/cycles/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FEB_2025"));
    }
}
