package com.giapho.coffee_shop_backend.util;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fluent builder for JPA Specifications. Provides a convenient way to build
 * complex queries with multiple criteria.
 *
 * @param <T> The entity type
 */
public class SpecificationBuilder<T> {

    private static final Locale NORMALIZE_LOCALE = Locale.ROOT;
    private final List<Specification<T>> specifications = new ArrayList<>();

    /**
     * Creates a new SpecificationBuilder instance.
     */
    public static <T> SpecificationBuilder<T> builder() {
        return new SpecificationBuilder<>();
    }

    /**
     * Adds a LIKE condition (case-insensitive) if the value is not empty.
     *
     * @param fieldName The field name to search
     * @param value The search value
     * @return this builder
     */
    public SpecificationBuilder<T> likeIgnoreCase(String fieldName, String value) {
        if (StringUtils.hasText(value)) {
            specifications.add((root, query, cb) -> {
                String keyword = "%" + value.trim().toLowerCase(NORMALIZE_LOCALE) + "%";
                return cb.like(cb.lower(root.get(fieldName)), keyword);
            });
        }
        return this;
    }

    /**
     * Adds an EQUAL condition if the value is not null.
     *
     * @param fieldName The field name
     * @param value The value
     * @return this builder
     */
    public SpecificationBuilder<T> equal(String fieldName, Object value) {
        if (value != null) {
            specifications.add((root, query, cb)
                    -> cb.equal(root.get(fieldName), value));
        }
        return this;
    }

    /**
     * Adds a nested EQUAL condition (e.g., "category.id").
     *
     * @param parentField The parent field name
     * @param childField The child field name
     * @param value The value
     * @return this builder
     */
    public SpecificationBuilder<T> equalNested(String parentField, String childField, Object value) {
        if (value != null) {
            specifications.add((root, query, cb)
                    -> cb.equal(root.get(parentField).get(childField), value));
        }
        return this;
    }

    /**
     * Adds a GREATER THAN OR EQUAL condition if the value is not null.
     *
     * @param fieldName The field name
     * @param value The minimum value
     * @return this builder
     */
    public <Y extends Comparable<? super Y>> SpecificationBuilder<T> greaterThanOrEqual(
            String fieldName, Y value) {
        if (value != null) {
            specifications.add((root, query, cb)
                    -> cb.greaterThanOrEqualTo(root.get(fieldName), value));
        }
        return this;
    }

    /**
     * Adds a LESS THAN OR EQUAL condition if the value is not null.
     *
     * @param fieldName The field name
     * @param value The maximum value
     * @return this builder
     */
    public <Y extends Comparable<? super Y>> SpecificationBuilder<T> lessThanOrEqual(
            String fieldName, Y value) {
        if (value != null) {
            specifications.add((root, query, cb)
                    -> cb.lessThanOrEqualTo(root.get(fieldName), value));
        }
        return this;
    }

    /**
     * Adds a BETWEEN condition if both values are not null.
     *
     * @param fieldName The field name
     * @param start The start value
     * @param end The end value
     * @return this builder
     */
    public <Y extends Comparable<? super Y>> SpecificationBuilder<T> between(
            String fieldName, Y start, Y end) {
        if (start != null && end != null) {
            specifications.add((root, query, cb)
                    -> cb.between(root.get(fieldName), start, end));
        }
        return this;
    }

    /**
     * Adds a custom specification.
     *
     * @param specification The custom specification
     * @return this builder
     */
    public SpecificationBuilder<T> and(Specification<T> specification) {
        if (specification != null) {
            specifications.add(specification);
        }
        return this;
    }

    /**
     * Builds the final Specification combining all added criteria with AND.
     *
     * @return The combined Specification
     */
    public Specification<T> build() {
        if (specifications.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> {
            Predicate[] predicates = specifications.stream()
                    .map(spec -> spec.toPredicate(root, query, cb))
                    .toArray(Predicate[]::new);
            return cb.and(predicates);
        };
    }
}
