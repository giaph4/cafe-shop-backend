package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Tìm khách hàng theo số điện thoại (quan trọng để tra cứu)
    Optional<Customer> findByPhone(String phone);

    // Kiểm tra tồn tại theo số điện thoại
    boolean existsByPhone(String phone);

    // Kiểm tra tồn tại theo email (nếu email là unique)
    boolean existsByEmail(String email);

    // Tìm kiếm khách hàng theo tên hoặc SĐT (phân trang)
    Page<Customer> findByFullNameContainingIgnoreCaseOrPhoneContaining(String fullName, String phone, Pageable pageable);
}