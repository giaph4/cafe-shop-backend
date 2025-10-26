package com.giapho.coffee_shop_backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime; // Thêm import
import java.util.Objects;
// Bỏ import Order nếu chưa dùng tới
// import java.util.Set;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String phone; // Số điện thoại (dùng làm khóa chính nghiệp vụ)

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(unique = true, length = 100)
    private String email; // Có thể null

    @Column(name = "loyalty_points")
    @Builder.Default
    private int loyaltyPoints = 0; // Điểm tích lũy, mặc định là 0

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // Thêm ngày tạo

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Thêm ngày cập nhật

    // --- (Quan hệ One-to-Many với Order sẽ thêm sau nếu cần) ---
    // @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    // @ToString.Exclude
    // private Set<Order> orders;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Ghi đè equals và hashCode
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Customer customer = (Customer) o;
        return getId() != null && Objects.equals(getId(), customer.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}