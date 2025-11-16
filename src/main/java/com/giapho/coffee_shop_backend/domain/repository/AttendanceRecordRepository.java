package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    @Query("SELECT ar FROM AttendanceRecord ar " +
            "WHERE ar.assignment.user.id = :userId " +
            "AND ar.assignment.shift.shiftDate BETWEEN :startDate AND :endDate")
    List<AttendanceRecord> findRecordsForUserBetweenDates(@Param("userId") Long userId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT ar FROM AttendanceRecord ar " +
            "WHERE ar.assignment.shift.id = :shiftId")
    List<AttendanceRecord> findByShiftId(@Param("shiftId") Long shiftId);

    @Query("SELECT ar FROM AttendanceRecord ar " +
            "WHERE ar.assignment.id = :assignmentId AND ar.checkInAt BETWEEN :start AND :end")
    List<AttendanceRecord> findByAssignmentWithinRange(@Param("assignmentId") Long assignmentId,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);

    List<AttendanceRecord> findByAssignmentId(Long assignmentId);

    Optional<AttendanceRecord> findFirstByAssignmentIdAndCheckOutAtIsNullOrderByCheckInAtDesc(Long assignmentId);

    List<AttendanceRecord> findByAssignmentIdIn(Collection<Long> assignmentIds);
}
