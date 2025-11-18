package com.giapho.coffee_shop_backend.service.cafetable.helper;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.enums.TableStatus;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.exception.cafetable.CafeTableConflictException;
import com.giapho.coffee_shop_backend.exception.cafetable.CafeTableDeletionNotAllowedException;
import com.giapho.coffee_shop_backend.exception.cafetable.CafeTableNotFoundException;
import com.giapho.coffee_shop_backend.exception.cafetable.CafeTableValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Chứa các hàm kiểm tra/chuẩn hoá dữ liệu nghiệp vụ cho module bàn.
 */
@Component
@RequiredArgsConstructor
public class CafeTableValidator {

    private final CafeTableRepository cafeTableRepository;
    private final OrderRepository orderRepository;

    public CafeTable requireExistingTable(Long id) {
        return cafeTableRepository.findById(id)
                .orElseThrow(() -> new CafeTableNotFoundException(id));
    }

    public String normalizeName(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            throw new CafeTableValidationException("Tên bàn không được để trống");
        }
        return rawName.trim().replaceAll("\\s+", " ");
    }

    public void ensureNameUnique(String normalizedName, Long currentTableId) {
        cafeTableRepository.findByName(normalizedName)
                .filter(existing -> !Objects.equals(existing.getId(), currentTableId))
                .ifPresent(existing -> {
                    throw new CafeTableConflictException(normalizedName);
                });
    }

    public TableStatus parseStatus(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            throw new CafeTableValidationException("Trạng thái bàn không được để trống");
        }
        try {
            return TableStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            String allowed = Arrays.stream(TableStatus.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new CafeTableValidationException("Trạng thái không hợp lệ. Giá trị hợp lệ: " + allowed);
        }
    }

    public void ensureTableDeletable(Long tableId) {
        long orderCount = orderRepository.countByCafeTableId(tableId);
        if (orderCount > 0) {
            throw new CafeTableDeletionNotAllowedException(tableId);
        }
    }
}
