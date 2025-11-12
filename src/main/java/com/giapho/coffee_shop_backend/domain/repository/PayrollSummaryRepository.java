package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.PayrollSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollSummaryRepository extends JpaRepository<PayrollSummary, Long> {

    Optional<PayrollSummary> findByCycleIdAndUserId(Long cycleId, Long userId);

    @Query("SELECT ps FROM PayrollSummary ps " +
            "JOIN FETCH ps.user u " +
            "JOIN FETCH ps.cycle c " +
            "WHERE (:cycleId IS NULL OR c.id = :cycleId) " +
            "AND (:userId IS NULL OR u.id = :userId) " +
            "ORDER BY u.fullName ASC, u.username ASC")
    List<PayrollSummary> search(@Param("cycleId") Long cycleId,
                                @Param("userId") Long userId);

    void deleteByCycleId(Long cycleId);
}
