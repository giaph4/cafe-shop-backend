package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.ShiftSession;
import com.giapho.coffee_shop_backend.dto.shift.ShiftReportResponseDTO;

import java.util.List;

public interface ShiftReportService {

    ShiftReportResponseDTO generateReport(ShiftSession session, List<Order> orders);

    ShiftReportResponseDTO getReport(Long sessionId);

    ShiftReportResponseDTO getOrGenerate(Long sessionId);

    List<ShiftReportResponseDTO> listReportsByWorkShift(Long workShiftId);

    ShiftReportResponseDTO regenerateReport(Long sessionId);
}
