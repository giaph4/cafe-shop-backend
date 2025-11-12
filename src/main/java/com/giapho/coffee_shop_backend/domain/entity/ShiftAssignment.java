package com.giapho.coffee_shop_backend.domain.entity;

import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shift_assignments")
public class ShiftAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_instance_id", nullable = false)
    @ToString.Exclude
    private ShiftInstance shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(name = "role_name", length = 50)
    private String roleName;

    @Column(name = "planned_start", nullable = false)
    private LocalTime plannedStart;

    @Column(name = "planned_end", nullable = false)
    private LocalTime plannedEnd;

    @Column(name = "planned_minutes", nullable = false)
    private Integer plannedMinutes;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(name = "fixed_allowance")
    private BigDecimal fixedAllowance;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ShiftAssignmentStatus status = ShiftAssignmentStatus.SCHEDULED;

    @Column(name = "actual_minutes")
    private Integer actualMinutes;

    @Column(name = "total_orders")
    private Integer totalOrders;

    @Column(name = "total_revenue")
    private BigDecimal totalRevenue;

    @Column(name = "bonus_amount")
    private BigDecimal bonusAmount;

    @Column(name = "penalty_amount")
    private BigDecimal penaltyAmount;

    @Column(name = "base_payroll")
    private BigDecimal basePayroll;

    @Column(name = "adjustment_total")
    private BigDecimal adjustmentTotal;

    @Column(name = "calculated_payroll")
    private BigDecimal calculatedPayroll;

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

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private Set<AttendanceRecord> attendanceRecords = new HashSet<>();

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private Set<ShiftPerformanceAdjustment> adjustments = new HashSet<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        normalizeNumericFields();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalizeNumericFields();
    }

    private void normalizeNumericFields() {
        if (hourlyRate == null) {
            hourlyRate = BigDecimal.ZERO;
        }
        if (fixedAllowance == null) {
            fixedAllowance = BigDecimal.ZERO;
        }
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        if (bonusAmount == null) {
            bonusAmount = BigDecimal.ZERO;
        }
        if (penaltyAmount == null) {
            penaltyAmount = BigDecimal.ZERO;
        }
        if (basePayroll == null) {
            basePayroll = BigDecimal.ZERO;
        }
        if (adjustmentTotal == null) {
            adjustmentTotal = BigDecimal.ZERO;
        }
        if (calculatedPayroll == null) {
            calculatedPayroll = BigDecimal.ZERO;
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ShiftAssignment that = (ShiftAssignment) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
