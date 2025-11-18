package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "roles")
    Page<User> findAll(Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("SELECT new com.giapho.coffee_shop_backend.dto.StaffPerformanceDTO(" +
            "  u.id, " +
            "  u.username, " +
            "  u.fullName, " +
            "  CAST(COUNT(o.id) AS long), " +
            "  CAST(COALESCE(SUM(o.totalAmount), 0) AS java.math.BigDecimal), " +
            "  CAST(COALESCE(AVG(o.totalAmount), 0) AS java.math.BigDecimal), " +
            "  'STAFF') " +
            "FROM User u " +
            "LEFT JOIN Order o ON o.user.id = u.id AND o.status = 'PAID' AND o.paidAt BETWEEN :startDate AND :endDate " +
            "GROUP BY u.id, u.username, u.fullName " +
            "ORDER BY COALESCE(SUM(o.totalAmount), 0) DESC")
    List<com.giapho.coffee_shop_backend.dto.StaffPerformanceDTO> findStaffPerformanceBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}

