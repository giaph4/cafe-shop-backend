package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.ShiftPerformanceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftPerformanceAdjustmentRepository extends JpaRepository<ShiftPerformanceAdjustment, Long> {
    List<ShiftPerformanceAdjustment> findByAssignmentId(Long assignmentId);
}
