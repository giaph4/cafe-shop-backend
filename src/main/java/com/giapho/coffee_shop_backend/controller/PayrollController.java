package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.domain.enums.PayrollCycleStatus;
import com.giapho.coffee_shop_backend.dto.shift.PayrollCycleRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.PayrollCycleResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.PayrollSummaryDTO;
import com.giapho.coffee_shop_backend.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/cycles")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<PayrollCycleResponseDTO>> listCycles(
            @RequestParam(required = false) PayrollCycleStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(payrollService.searchCycles(status, from, to));
    }

    @GetMapping("/cycles/{cycleId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<PayrollCycleResponseDTO> getCycle(@PathVariable Long cycleId) {
        return ResponseEntity.ok(payrollService.getCycle(cycleId));
    }

    @PostMapping("/cycles")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<PayrollCycleResponseDTO> createCycle(@Valid @RequestBody PayrollCycleRequestDTO request) {
        PayrollCycleResponseDTO created = payrollService.createCycle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/cycles/{cycleId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<PayrollCycleResponseDTO> updateCycle(
            @PathVariable Long cycleId,
            @Valid @RequestBody PayrollCycleRequestDTO request
    ) {
        PayrollCycleResponseDTO updated = payrollService.updateCycle(cycleId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/cycles/{cycleId}/regenerate")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<PayrollSummaryDTO>> regenerateSummaries(@PathVariable Long cycleId) {
        List<PayrollSummaryDTO> summaries = payrollService.regenerateSummaries(cycleId);
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/summaries")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<PayrollSummaryDTO>> listSummaries(
            @RequestParam(required = false) Long cycleId,
            @RequestParam(required = false) Long userId
    ) {
        return ResponseEntity.ok(payrollService.getSummaries(cycleId, userId));
    }
}
