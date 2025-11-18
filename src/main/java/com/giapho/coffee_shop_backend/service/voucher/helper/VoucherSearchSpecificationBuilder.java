package com.giapho.coffee_shop_backend.service.voucher.helper;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class VoucherSearchSpecificationBuilder {

    public Specification<Voucher> build(String code,
                                        Voucher.VoucherType type,
                                        Boolean active,
                                        LocalDateTime validFrom,
                                        LocalDateTime validTo) {
        List<Specification<Voucher>> predicates = new ArrayList<>();

        if (StringUtils.hasText(code)) {
            String keyword = "%" + code.trim().toLowerCase(Locale.ROOT) + "%";
            predicates.add((root, query, cb) -> cb.like(cb.lower(root.get("code")), keyword));
        }
        if (type != null) {
            predicates.add((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (active != null) {
            predicates.add((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        if (validFrom != null) {
            predicates.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("validFrom"), validFrom));
        }
        if (validTo != null) {
            predicates.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("validTo"), validTo));
        }

        if (predicates.isEmpty()) {
            return null;
        }
        return Specification.allOf(predicates);
    }
}
