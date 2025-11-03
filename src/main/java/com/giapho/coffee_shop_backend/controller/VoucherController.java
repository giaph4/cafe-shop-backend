package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import com.giapho.coffee_shop_backend.service.VoucherService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/check")
    public ResponseEntity<VoucherCheckResponseDTO> checkVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal amount) {
        try {
            VoucherCheckResponseDTO response = voucherService.checkAndCalculateDiscount(code, amount);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.ok(VoucherCheckResponseDTO.builder()
                    .isValid(false)
                    .message(e.getMessage())
                    .code(code)
                    .discountAmount(BigDecimal.ZERO)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(VoucherCheckResponseDTO.builder()
                    .isValid(false)
                    .message("Lỗi hệ thống khi kiểm tra voucher.")
                    .code(code)
                    .discountAmount(BigDecimal.ZERO)
                    .build());
        }
    }
}