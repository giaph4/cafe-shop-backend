package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.ShiftReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ShiftReportRepository extends JpaRepository<ShiftReport, Long> {

    @EntityGraph(attributePaths = {"session", "session.user", "session.workShift"})
    Optional<ShiftReport> findBySession_Id(Long sessionId);

    @EntityGraph(attributePaths = {"session", "session.user", "session.workShift"})
    List<ShiftReport> findBySession_WorkShift_Id(Long workShiftId);
}
