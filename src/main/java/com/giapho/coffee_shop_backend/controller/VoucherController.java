package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import com.giapho.coffee_shop_backend.dto.VoucherRequestDTO;
import com.giapho.coffee_shop_backend.dto.VoucherResponseDTO;
import com.giapho.coffee_shop_backend.dto.VoucherSummaryDTO;
import com.giapho.coffee_shop_backend.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<VoucherCheckResponseDTO> checkVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal amount) {
        VoucherCheckResponseDTO response = voucherService.checkAndCalculateDiscount(code, amount);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<VoucherResponseDTO> createVoucher(@Valid @RequestBody VoucherRequestDTO requestDTO) {
        VoucherResponseDTO response = voucherService.createVoucher(requestDTO);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<VoucherResponseDTO> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody VoucherRequestDTO requestDTO
    ) {
        VoucherResponseDTO response = voucherService.updateVoucher(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<VoucherResponseDTO> toggleVoucher(@PathVariable Long id) {
        VoucherResponseDTO response = voucherService.toggleVoucherActive(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<VoucherResponseDTO>> searchVouchers(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Voucher.VoucherType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validTo,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        Page<VoucherResponseDTO> result = voucherService.searchVouchers(code, type, active, validFrom, validTo, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<VoucherResponseDTO> getVoucherById(@PathVariable Long id) {
        VoucherResponseDTO response = voucherService.getVoucherById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<VoucherSummaryDTO> getVoucherSummary() {
        VoucherSummaryDTO summary = voucherService.getVoucherSummary();
        return ResponseEntity.ok(summary);
    }
}