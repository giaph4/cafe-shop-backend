package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    @Query("SELECT COUNT(sa) > 0 FROM ShiftAssignment sa " +
            "WHERE sa.user.id = :userId " +
            "AND sa.shift.shiftDate = :shiftDate " +
            "AND ((sa.plannedStart <= :endTime AND sa.plannedEnd >= :startTime))")
    boolean hasOverlappingAssignment(@Param("userId") Long userId,
                                     @Param("shiftDate") LocalDate shiftDate,
                                     @Param("startTime") LocalTime startTime,
                                     @Param("endTime") LocalTime endTime);

    @Query("SELECT COUNT(sa) > 0 FROM ShiftAssignment sa " +
            "WHERE sa.id <> :assignmentId " +
            "AND sa.user.id = :userId " +
            "AND sa.shift.shiftDate = :shiftDate " +
            "AND ((sa.plannedStart <= :endTime AND sa.plannedEnd >= :startTime))")
    boolean hasOverlappingAssignmentExcludingId(@Param("assignmentId") Long assignmentId,
                                                @Param("userId") Long userId,
                                                @Param("shiftDate") LocalDate shiftDate,
                                                @Param("startTime") LocalTime startTime,
                                                @Param("endTime") LocalTime endTime);

    @EntityGraph(attributePaths = {"shift", "user"})
    List<ShiftAssignment> findByShiftId(Long shiftId);

    @EntityGraph(attributePaths = {"shift", "user"})
    List<ShiftAssignment> findByUserIdOrderByShift_ShiftDateDesc(Long userId);

    @EntityGraph(attributePaths = "shift")
    List<ShiftAssignment> findByUserIdAndShift_ShiftDateBetween(Long userId, LocalDate start, LocalDate end);

    List<ShiftAssignment> findByUserIdAndShift_ShiftDate(Long userId, LocalDate date);

    Optional<ShiftAssignment> findByShiftIdAndUserId(Long shiftId, Long userId);

    List<ShiftAssignment> findByStatus(ShiftAssignmentStatus status);

    @EntityGraph(attributePaths = {"shift", "user"})
    List<ShiftAssignment> findByShift_ShiftDateBetween(LocalDate start, LocalDate end);

    boolean existsByShiftId(Long shiftId);
}
