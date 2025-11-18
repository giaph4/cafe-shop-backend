package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.enums.PayrollCycleStatus;
import com.giapho.coffee_shop_backend.dto.shift.PayrollCycleRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.PayrollCycleResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.PayrollSummaryDTO;

import java.time.LocalDate;
import java.util.List;

public interface PayrollService {

    PayrollCycleResponseDTO createCycle(PayrollCycleRequestDTO request);

    PayrollCycleResponseDTO updateCycle(Long cycleId, PayrollCycleRequestDTO request);

    PayrollCycleResponseDTO getCycle(Long cycleId);

    List<PayrollCycleResponseDTO> searchCycles(PayrollCycleStatus status, LocalDate from, LocalDate to);

    List<PayrollSummaryDTO> regenerateSummaries(Long cycleId);

    List<PayrollSummaryDTO> getSummaries(Long cycleId, Long userId);
}
