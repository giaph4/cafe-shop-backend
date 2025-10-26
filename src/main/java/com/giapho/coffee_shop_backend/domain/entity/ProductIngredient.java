package com.giapho.coffee_shop_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "product_ingredients")
public class ProductIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Dùng ID riêng cho dòng định lượng này

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false) // Món ăn nào?
    @ToString.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false) // Cần nguyên liệu nào?
    @ToString.Exclude
    private Ingredient ingredient;

    @Column(name = "quantity_needed", nullable = false)
    private BigDecimal quantityNeeded; // Cần bao nhiêu? (Đơn vị tính theo Ingredient.unit)

    // Ghi đè equals và hashCode
    // Quan trọng: equals/hashCode nên dựa trên Product và Ingredient (hoặc ID nếu đã có)
    // để tránh thêm trùng lặp cùng một nguyên liệu cho cùng một sản phẩm.
    // Tuy nhiên, cách đơn giản nhất vẫn là dựa trên ID cho nhất quán.
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ProductIngredient that = (ProductIngredient) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}