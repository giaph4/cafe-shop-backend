package com.giapho.coffee_shop_backend.service.purchaseorder.helper;

import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class PurchaseOrderSpecificationBuilder {

    public Specification<PurchaseOrder> build(String status,
                                              Long supplierId,
                                              LocalDate startDate,
                                              LocalDate endDate) {
        return (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank()) {
                predicates.add(builder.equal(root.get("status"), status));
            }

            if (supplierId != null) {
                predicates.add(builder.equal(root.get("supplier").get("id"), supplierId));
            }

            if (startDate != null) {
                LocalDateTime start = startDate.atStartOfDay();
                predicates.add(builder.greaterThanOrEqualTo(root.get("orderDate"), start));
            }

            if (endDate != null) {
                LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
                predicates.add(builder.lessThanOrEqualTo(root.get("orderDate"), endDateTime));
            }

            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
