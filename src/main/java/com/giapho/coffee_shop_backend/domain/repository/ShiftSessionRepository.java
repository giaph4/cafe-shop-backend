package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.ShiftSession;
import com.giapho.coffee_shop_backend.domain.enums.ShiftSessionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftSessionRepository extends JpaRepository<ShiftSession, Long> {

    Optional<ShiftSession> findFirstByUser_IdAndStatus(Long userId, ShiftSessionStatus status);

    long countByWorkShift_IdAndStatus(Long workShiftId, ShiftSessionStatus status);

    List<ShiftSession> findByWorkShift_IdAndStatus(Long workShiftId, ShiftSessionStatus status);

    List<ShiftSession> findByStartAtBetween(LocalDateTime start, LocalDateTime end);

    List<ShiftSession> findByStatus(ShiftSessionStatus status);

    @EntityGraph(attributePaths = {"user", "workShift"})
    Optional<ShiftSession> findWithUserAndWorkShiftById(Long sessionId);
}
