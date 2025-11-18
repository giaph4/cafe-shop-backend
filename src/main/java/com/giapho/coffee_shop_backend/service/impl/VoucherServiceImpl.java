package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.domain.repository.VoucherRepository;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import com.giapho.coffee_shop_backend.dto.VoucherRequestDTO;
import com.giapho.coffee_shop_backend.dto.VoucherResponseDTO;
import com.giapho.coffee_shop_backend.dto.VoucherSummaryDTO;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherInvalidException;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherNotFoundException;
import com.giapho.coffee_shop_backend.service.VoucherService;
import com.giapho.coffee_shop_backend.service.voucher.helper.VoucherDiscountCalculator;
import com.giapho.coffee_shop_backend.service.voucher.helper.VoucherMapper;
import com.giapho.coffee_shop_backend.service.voucher.helper.VoucherSearchSpecificationBuilder;
import com.giapho.coffee_shop_backend.service.voucher.helper.VoucherValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherValidator voucherValidator;
    private final VoucherMapper voucherMapper;
    private final VoucherDiscountCalculator discountCalculator;
    private final VoucherSearchSpecificationBuilder specificationBuilder;

    @Override
    public VoucherCheckResponseDTO checkAndCalculateDiscount(String code, BigDecimal orderAmount) {
        voucherValidator.validateDiscountRequest(code, orderAmount);
        String normalizedCode = voucherValidator.normalizeCode(code);

        return voucherRepository.findByCodeIgnoreCase(normalizedCode)
                .map(voucher -> discountCalculator.evaluate(voucher, orderAmount))
                .orElseGet(() -> discountCalculator.buildNotFoundResponse(code));
    }

    @Override
    public VoucherResponseDTO getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new VoucherNotFoundException(id));
        return voucherMapper.toResponse(voucher);
    }

    @Override
    @Transactional
    public VoucherResponseDTO createVoucher(VoucherRequestDTO request) {
        String normalizedCode = voucherValidator.normalizeCode(request.getCode());
        voucherValidator.ensureCodeUnique(normalizedCode, voucherRepository::existsByCodeIgnoreCase);
        voucherValidator.validateBusinessRules(request);

        Voucher voucher = voucherMapper.toEntity(request);
        voucher.setCode(normalizedCode);
        voucher.setTimesUsed(0);

        Voucher saved = voucherRepository.save(voucher);
        log.info("Created voucher {}", saved.getCode());
        return voucherMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VoucherResponseDTO updateVoucher(Long id, VoucherRequestDTO request) {
        Voucher existing = voucherRepository.findById(id)
                .orElseThrow(() -> new VoucherNotFoundException(id));

        String normalizedCode = voucherValidator.normalizeCode(request.getCode());
        voucherValidator.ensureCodeAvailableForUpdate(existing, normalizedCode, voucherRepository::existsByCodeIgnoreCase);
        voucherValidator.validateBusinessRules(request);
        voucherValidator.ensureUsageLimitNotLessThanTimesUsed(existing, request.getUsageLimit());

        voucherMapper.updateEntity(request, existing);
        existing.setCode(normalizedCode);
        Voucher saved = voucherRepository.save(existing);
        log.info("Updated voucher {}", saved.getCode());
        return voucherMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VoucherResponseDTO toggleVoucherActive(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new VoucherNotFoundException(id));

        voucher.setActive(!voucher.isActive());
        voucher.setUpdatedAt(LocalDateTime.now());

        Voucher saved = voucherRepository.save(voucher);
        log.info("Toggled voucher {} active state to {}", saved.getCode(), saved.isActive());
        return voucherMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new VoucherNotFoundException(id));

        if (voucher.getTimesUsed() > 0) {
            throw new VoucherInvalidException("Không thể xóa voucher đã được sử dụng");
        }

        voucherRepository.delete(voucher);
        log.info("Deleted voucher {}", voucher.getCode());
    }

    @Override
    public Page<VoucherResponseDTO> searchVouchers(String code,
                                                   Voucher.VoucherType type,
                                                   Boolean active,
                                                   LocalDateTime validFrom,
                                                   LocalDateTime validTo,
                                                   Pageable pageable) {
        Specification<Voucher> specification = specificationBuilder.build(code, type, active, validFrom, validTo);
        return (specification == null
                ? voucherRepository.findAll(pageable)
                : voucherRepository.findAll(specification, pageable))
                .map(voucherMapper::toResponse);
    }

    @Override
    public VoucherSummaryDTO getVoucherSummary() {
        long totalActive = voucherRepository.countByActiveTrue();
        long totalInactive = voucherRepository.countByActiveFalse();
        LocalDateTime now = LocalDateTime.now();
        long expiringSoon = voucherRepository.countByValidToBetween(now, now.plusDays(7));
        long totalRedeemed = voucherRepository.sumTimesUsed();

        return VoucherSummaryDTO.builder()
                .activeCount(totalActive)
                .inactiveCount(totalInactive)
                .expiringSoonCount(expiringSoon)
                .redeemedCount(totalRedeemed)
                .build();
    }

    @Override
    @Transactional
    public void incrementUsageCount(String code) {
        if (!voucherValidator.hasText(code)) {
            return;
        }
        String normalizedCode = voucherValidator.normalizeCode(code);
        voucherRepository.findByCodeIgnoreCase(normalizedCode).ifPresent(voucher -> {
            voucher.setTimesUsed(voucher.getTimesUsed() + 1);
            voucher.setUpdatedAt(LocalDateTime.now());
            voucherRepository.save(voucher);
        });
    }
}
