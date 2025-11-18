package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import com.giapho.coffee_shop_backend.dto.VoucherRequestDTO;
import com.giapho.coffee_shop_backend.dto.VoucherResponseDTO;
import com.giapho.coffee_shop_backend.dto.VoucherSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface VoucherService {

    VoucherCheckResponseDTO checkAndCalculateDiscount(String code, BigDecimal orderAmount);

    VoucherResponseDTO getVoucherById(Long id);

    VoucherResponseDTO createVoucher(VoucherRequestDTO request);

    VoucherResponseDTO updateVoucher(Long id, VoucherRequestDTO request);

    VoucherResponseDTO toggleVoucherActive(Long id);

    void deleteVoucher(Long id);

    Page<VoucherResponseDTO> searchVouchers(String code,
                                            Voucher.VoucherType type,
                                            Boolean active,
                                            LocalDateTime validFrom,
                                            LocalDateTime validTo,
                                            Pageable pageable);

    VoucherSummaryDTO getVoucherSummary();

    void incrementUsageCount(String code);
}
