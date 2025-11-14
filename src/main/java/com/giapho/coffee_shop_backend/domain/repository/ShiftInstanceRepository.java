package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShiftInstanceRepository extends JpaRepository<ShiftInstance, Long>, JpaSpecificationExecutor<ShiftInstance> {

    List<ShiftInstance> findByShiftDateBetween(LocalDate start, LocalDate end);

    List<ShiftInstance> findByShiftDateBetweenAndStatus(LocalDate start, LocalDate end, ShiftStatus status);

    @Query("SELECT COUNT(si) > 0 FROM ShiftInstance si WHERE si.template.id = :templateId AND si.shiftDate = :shiftDate")
    boolean existsByTemplateAndDate(@Param("templateId") Long templateId,
                                    @Param("shiftDate") LocalDate shiftDate);

    @Query("""
            SELECT DISTINCT si FROM ShiftInstance si
            LEFT JOIN FETCH si.assignments assignments
            LEFT JOIN FETCH si.template template
            WHERE si.shiftDate BETWEEN :start AND :end
            """)
    List<ShiftInstance> findWithTemplateAndAssignmentsBetween(@Param("start") LocalDate start,
                                                              @Param("end") LocalDate end);
}
