package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.PayrollCycle;
import com.giapho.coffee_shop_backend.domain.enums.PayrollCycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollCycleRepository extends JpaRepository<PayrollCycle, Long> {

    Optional<PayrollCycle> findByCode(String code);

    @Query("SELECT c FROM PayrollCycle c WHERE (:status IS NULL OR c.status = :status) " +
            "AND (:from IS NULL OR c.startDate >= :from) " +
            "AND (:to IS NULL OR c.endDate <= :to) " +
            "ORDER BY c.startDate DESC")
    List<PayrollCycle> search(@Param("status") PayrollCycleStatus status,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);
}
