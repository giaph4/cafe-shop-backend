package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    List<WorkShift> findByIsActiveTrueOrderByStartAtAsc();

    List<WorkShift> findByStartAtLessThanEqualAndEndAtGreaterThanEqual(LocalDateTime end,
                                                                        LocalDateTime start);
}
