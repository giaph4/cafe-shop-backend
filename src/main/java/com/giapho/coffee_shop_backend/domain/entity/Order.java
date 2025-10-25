package com.giapho.coffee_shop_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "orders") // Tên bảng là "orders"
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ----- MỐI QUAN HỆ VỚI BÀN -----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    @ToString.Exclude
    private CafeTable cafeTable;

    // ----- MỐI QUAN HỆ VỚI NHÂN VIÊN -----
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    // (Chúng ta sẽ thêm 'customer' sau)

    @Column(nullable = false, length = 20)
    private String type; // Loại đơn: AT_TABLE, TAKE_AWAY, DELIVERY

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // Trạng thái: PENDING, PAID, CANCELLED

    @Column(name = "sub_total", nullable = false)
    private BigDecimal subTotal; // Tổng tiền (trước giảm giá)

    @Column(name = "discount_amount")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO; // Tiền giảm giá

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount; // Tổng tiền cuối cùng

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_method", length = 20) // Lưu 'CASH', 'TRANSFER', 'CARD', etc.
    private String paymentMethod;

    /**
     * ----- MỐI QUAN HỆ VỚI CHI TIẾT ĐƠN HÀNG -----
     *  Một Order có nhiều OrderDetail
     *  CascadeType.ALL: Khi lưu/xoá Order, tự động lưu/xoá OrderDetail
     *  orphanRemoval = true: Khi xoá 1 item khỏi Set, tự động xoá nó trong DB
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<OrderDetail> orderDetails;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Order order = (Order) o;
        return getId() != null && Objects.equals(getId(), order.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}