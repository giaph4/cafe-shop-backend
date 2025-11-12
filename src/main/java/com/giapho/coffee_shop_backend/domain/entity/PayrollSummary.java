package com.giapho.coffee_shop_backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.proxy.HibernateProxy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payroll_summaries")
public class PayrollSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    @ToString.Exclude
    private PayrollCycle cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "assignment_count", nullable = false)
    private Integer assignmentCount;

    @Column(name = "attendance_count", nullable = false)
    private Integer attendanceCount;

    @Column(name = "total_actual_minutes", nullable = false)
    private Integer totalActualMinutes;

    @Column(name = "total_orders", nullable = false)
    private Integer totalOrders;

    @Column(name = "total_revenue", nullable = false)
    private BigDecimal totalRevenue;

    @Column(name = "total_base_payroll", nullable = false)
    private BigDecimal totalBasePayroll;

    @Column(name = "total_bonus", nullable = false)
    private BigDecimal totalBonus;

    @Column(name = "total_penalty", nullable = false)
    private BigDecimal totalPenalty;

    @Column(name = "total_adjustment", nullable = false)
    private BigDecimal totalAdjustment;

    @Column(name = "total_net_payroll", nullable = false)
    private BigDecimal totalNetPayroll;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        normalizeNumericFields();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalizeNumericFields();
    }

    private void normalizeNumericFields() {
        if (assignmentCount == null) {
            assignmentCount = 0;
        }
        if (attendanceCount == null) {
            attendanceCount = 0;
        }
        if (totalActualMinutes == null) {
            totalActualMinutes = 0;
        }
        if (totalOrders == null) {
            totalOrders = 0;
        }
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        if (totalBasePayroll == null) {
            totalBasePayroll = BigDecimal.ZERO;
        }
        if (totalBonus == null) {
            totalBonus = BigDecimal.ZERO;
        }
        if (totalPenalty == null) {
            totalPenalty = BigDecimal.ZERO;
        }
        if (totalAdjustment == null) {
            totalAdjustment = BigDecimal.ZERO;
        }
        if (totalNetPayroll == null) {
            totalNetPayroll = BigDecimal.ZERO;
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PayrollSummary that = (PayrollSummary) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
