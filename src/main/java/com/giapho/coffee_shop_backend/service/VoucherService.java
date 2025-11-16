package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.domain.repository.VoucherRepository;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import com.giapho.coffee_shop_backend.dto.VoucherRequestDTO;
import com.giapho.coffee_shop_backend.dto.VoucherResponseDTO;
import com.giapho.coffee_shop_backend.dto.VoucherSummaryDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;

    @Transactional(readOnly = true)
    public VoucherCheckResponseDTO checkAndCalculateDiscount(String code, BigDecimal orderAmount) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Voucher code must not be empty");
        }

        if (orderAmount == null || orderAmount.signum() < 0) {
            throw new IllegalArgumentException("Order amount must be a positive number");
        }

        String normalizedCode = normalizeCode(code);

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new EntityNotFoundException("Voucher không tồn tại: " + code));

        if (!voucher.isActive()) {
            return buildInvalidResponse(voucher, "Voucher không hoạt động.");
        }
        if (voucher.getTimesUsed() >= voucher.getUsageLimit()) {
            return buildInvalidResponse(voucher, "Voucher đã hết lượt sử dụng.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getValidFrom())) {
            return buildInvalidResponse(voucher, "Voucher chưa đến ngày sử dụng.");
        }
        if (now.isAfter(voucher.getValidTo())) {
            return buildInvalidResponse(voucher, "Voucher đã hết hạn.");
        }
        if (voucher.getMinimumOrderAmount() != null && orderAmount.compareTo(voucher.getMinimumOrderAmount()) < 0) {
            return buildInvalidResponse(voucher, "Đơn hàng chưa đạt giá trị tối thiểu (" + voucher.getMinimumOrderAmount() + ").");
        }

        BigDecimal discountAmount = calculateDiscount(voucher, orderAmount);

        return VoucherCheckResponseDTO.builder()
                .isValid(true)
                .message("Áp dụng voucher thành công!")
                .code(voucher.getCode())
                .discountAmount(discountAmount)
                .type(voucher.getType())
                .build();
    }

    @Transactional(readOnly = true)
    public VoucherResponseDTO getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voucher không tồn tại: " + id));
        return mapToResponse(voucher);
    }

    @Transactional
    public VoucherResponseDTO createVoucher(VoucherRequestDTO request) {
        String normalizedCode = normalizeCode(request.getCode());
        if (voucherRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new DataIntegrityViolationException("Voucher code đã tồn tại: " + normalizedCode);
        }

        validateBusinessRules(request);

        Voucher voucher = new Voucher();
        applyRequestToEntity(request, voucher);
        voucher.setCode(normalizedCode);
        voucher.setTimesUsed(0);
        voucher.setCreatedAt(LocalDateTime.now());
        voucher.setUpdatedAt(LocalDateTime.now());

        Voucher saved = voucherRepository.save(voucher);
        log.info("Created voucher {}", saved.getCode());
        return mapToResponse(saved);
    }

    @Transactional
    public VoucherResponseDTO updateVoucher(Long id, VoucherRequestDTO request) {
        Voucher existing = voucherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voucher không tồn tại: " + id));

        String normalizedCode = normalizeCode(request.getCode());
        if (!existing.getCode().equalsIgnoreCase(normalizedCode)
                && voucherRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new DataIntegrityViolationException("Voucher code đã tồn tại: " + normalizedCode);
        }

        validateBusinessRules(request);

        if (existing.getTimesUsed() > request.getUsageLimit()) {
            throw new IllegalArgumentException("usageLimit không thể nhỏ hơn số lượt đã sử dụng hiện tại");
        }

        applyRequestToEntity(request, existing);
        existing.setCode(normalizedCode);
        existing.setUpdatedAt(LocalDateTime.now());

        Voucher saved = voucherRepository.save(existing);
        log.info("Updated voucher {}", saved.getCode());
        return mapToResponse(saved);
    }

    @Transactional
    public VoucherResponseDTO toggleVoucherActive(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voucher không tồn tại: " + id));

        voucher.setActive(!voucher.isActive());
        voucher.setUpdatedAt(LocalDateTime.now());

        Voucher saved = voucherRepository.save(voucher);
        log.info("Toggled voucher {} active state to {}", saved.getCode(), saved.isActive());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voucher không tồn tại: " + id));

        if (voucher.getTimesUsed() > 0) {
            throw new IllegalStateException("Không thể xóa voucher đã được sử dụng");
        }

        voucherRepository.delete(voucher);
        log.info("Deleted voucher {}", voucher.getCode());
    }

    @Transactional(readOnly = true)
    public Page<VoucherResponseDTO> searchVouchers(
            String code,
            Voucher.VoucherType type,
            Boolean active,
            LocalDateTime validFrom,
            LocalDateTime validTo,
            Pageable pageable
    ) {
        Specification<Voucher> specification = Specification.allOf();

        if (StringUtils.hasText(code)) {
            String keyword = "%" + code.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("code")), keyword));
        }

        if (type != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        if (active != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }

        if (validFrom != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("validFrom"), validFrom));
        }

        if (validTo != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("validTo"), validTo));
        }

        Page<Voucher> page = voucherRepository.findAll(specification, pageable);
        return page.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public VoucherSummaryDTO getVoucherSummary() {
        long totalActive = voucherRepository.countByActiveTrue();
        long totalInactive = voucherRepository.countByActiveFalse();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inSevenDays = now.plusDays(7);
        long expiringSoon = voucherRepository.countByValidToBetween(now, inSevenDays);
        long totalRedeemed = voucherRepository.sumTimesUsed();

        return VoucherSummaryDTO.builder()
                .activeCount(totalActive)
                .inactiveCount(totalInactive)
                .expiringSoonCount(expiringSoon)
                .redeemedCount(totalRedeemed)
                .build();
    }

    private void applyRequestToEntity(VoucherRequestDTO request, Voucher voucher) {
        voucher.setDescription(safeTrim(request.getDescription()));
        voucher.setType(request.getType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinimumOrderAmount(request.getMinimumOrderAmount());
        voucher.setMaximumDiscountAmount(request.getMaximumDiscountAmount());
        voucher.setValidFrom(request.getValidFrom().truncatedTo(ChronoUnit.SECONDS));
        voucher.setValidTo(request.getValidTo().truncatedTo(ChronoUnit.SECONDS));
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setActive(request.getActive() != null ? request.getActive() : Boolean.TRUE);
    }

    private void validateBusinessRules(VoucherRequestDTO request) {
        if (!StringUtils.hasText(request.getCode())) {
            throw new IllegalArgumentException("Voucher code không được bỏ trống");
        }

        if (request.getValidFrom().isAfter(request.getValidTo())) {
            throw new IllegalArgumentException("validFrom phải trước validTo");
        }

        if (request.getType() == Voucher.VoucherType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Giá trị giảm theo phần trăm không được vượt quá 100");
        }

        if (request.getMaximumDiscountAmount() != null
                && request.getType() == Voucher.VoucherType.FIXED_AMOUNT
                && request.getMaximumDiscountAmount().compareTo(request.getDiscountValue()) < 0) {
            throw new IllegalArgumentException("maximumDiscountAmount không thể nhỏ hơn discountValue đối với voucher cố định");
        }
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Voucher code không được bỏ trống");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private VoucherResponseDTO mapToResponse(Voucher voucher) {
        return VoucherResponseDTO.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .description(voucher.getDescription())
                .type(voucher.getType())
                .discountValue(voucher.getDiscountValue())
                .minimumOrderAmount(voucher.getMinimumOrderAmount())
                .maximumDiscountAmount(voucher.getMaximumDiscountAmount())
                .validFrom(voucher.getValidFrom())
                .validTo(voucher.getValidTo())
                .usageLimit(voucher.getUsageLimit())
                .timesUsed(voucher.getTimesUsed())
                .active(voucher.isActive())
                .createdAt(voucher.getCreatedAt())
                .updatedAt(voucher.getUpdatedAt())
                .build();
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount) {
        BigDecimal discount = BigDecimal.ZERO;
        if (voucher.getType() == Voucher.VoucherType.FIXED_AMOUNT) {
            discount = voucher.getDiscountValue();
        } else if (voucher.getType() == Voucher.VoucherType.PERCENTAGE) {
            discount = orderAmount.multiply(voucher.getDiscountValue().divide(BigDecimal.valueOf(100)));

            if (voucher.getMaximumDiscountAmount() != null && discount.compareTo(voucher.getMaximumDiscountAmount()) > 0) {
                discount = voucher.getMaximumDiscountAmount();
            }
        }

        return discount.min(orderAmount);
    }

    private VoucherCheckResponseDTO buildInvalidResponse(Voucher voucher, String message) {
        return VoucherCheckResponseDTO.builder()
                .isValid(false)
                .message(message)
                .code(voucher.getCode())
                .discountAmount(BigDecimal.ZERO)
                .type(voucher.getType())
                .build();
    }

    public void incrementUsageCount(String code) {
        if (!StringUtils.hasText(code)) {
            return;
        }

        String normalizedCode = normalizeCode(code);

        voucherRepository.findByCodeIgnoreCase(normalizedCode).ifPresent(voucher -> {
            voucher.setTimesUsed(voucher.getTimesUsed() + 1);
            voucher.setUpdatedAt(LocalDateTime.now());
            voucherRepository.save(voucher);
        });
    }
}