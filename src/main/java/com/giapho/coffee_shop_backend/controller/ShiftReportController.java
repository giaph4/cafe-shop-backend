package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.shift.ShiftReportResponseDTO;
import com.giapho.coffee_shop_backend.service.shift.ShiftReportService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts/reports")
@RequiredArgsConstructor
@Validated
public class ShiftReportController {

    private final ShiftReportService shiftReportService;

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ShiftReportResponseDTO> getReport(
            @PathVariable @Min(1) Long sessionId,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh
    ) {
        ShiftReportResponseDTO response = refresh
                ? shiftReportService.regenerateReport(sessionId)
                : shiftReportService.getOrGenerate(sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/regenerate")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ShiftReportResponseDTO> regenerateReport(
            @PathVariable @Min(1) Long sessionId
    ) {
        ShiftReportResponseDTO response = shiftReportService.regenerateReport(sessionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/work-shifts/{workShiftId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<ShiftReportResponseDTO>> listReportsByWorkShift(
            @PathVariable @Min(1) Long workShiftId
    ) {
        List<ShiftReportResponseDTO> response = shiftReportService.listReportsByWorkShift(workShiftId);
        return ResponseEntity.ok(response);
    }
}
