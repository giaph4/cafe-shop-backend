package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Customer;
import com.giapho.coffee_shop_backend.domain.repository.CustomerRepository;
import com.giapho.coffee_shop_backend.dto.CustomerDTO;
import com.giapho.coffee_shop_backend.mapper.CustomerMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    /**
     * Lấy danh sách khách hàng (phân trang, tìm kiếm)
     */
    @Transactional(readOnly = true)
    public Page<CustomerDTO> searchCustomers(String keyword, Pageable pageable) {
        Page<Customer> customerPage;
        if (keyword != null && !keyword.isEmpty()) {
            customerPage = customerRepository.findByFullNameContainingIgnoreCaseOrPhoneContaining(keyword, keyword, pageable);
        } else {
            customerPage = customerRepository.findAll(pageable);
        }
        // Map Page<Entity> sang Page<DTO>
        return customerPage.map(customerMapper::toDto);
    }

    /**
     * Lấy chi tiết khách hàng theo ID
     */
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));
        return customerMapper.toDto(customer);
    }

    @Transactional
    public void updateLoyaltyPoints(Long customerId, BigDecimal totalAmount) {
        if (customerId == null || totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        // Tính số điểm thưởng theo mức
        int pointsToAdd = 0;
        
        if (totalAmount.compareTo(new BigDecimal("100000")) >= 0) {
            pointsToAdd = 50;  // Trên 100,000 VND: +50 điểm
        } else if (totalAmount.compareTo(new BigDecimal("50000")) >= 0) {
            pointsToAdd = 20;  // Trên 50,000 VND: +20 điểm
        } else if (totalAmount.compareTo(new BigDecimal("30000")) >= 0) {
            pointsToAdd = 10;  // Trên 30,000 VND: +10 điểm
        }
        
        if (pointsToAdd > 0) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
            
            int currentPoints = Objects.requireNonNullElse(customer.getLoyaltyPoints(), 0);
            customer.setLoyaltyPoints(currentPoints + pointsToAdd);
            customerRepository.save(customer);
            
            log.info("Đã cộng {} điểm (đơn hàng {} VNĐ) cho khách hàng {}. Tổng điểm hiện tại: {}", 
                    pointsToAdd, totalAmount, customerId, customer.getLoyaltyPoints());
        }
    }

    /**
     * Tìm khách hàng theo số điện thoại
     */
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerByPhone(String phone) {
        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with phone: " + phone));
        return customerMapper.toDto(customer);
    }

    /**
     * Tạo khách hàng mới
     */
    @Transactional
    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        // Kiểm tra SĐT đã tồn tại chưa
        if (customerRepository.existsByPhone(customerDTO.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists: " + customerDTO.getPhone());
        }
        // Kiểm tra Email đã tồn tại chưa (nếu email được cung cấp)
        if (customerDTO.getEmail() != null && !customerDTO.getEmail().isEmpty() &&
                customerRepository.existsByEmail(customerDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + customerDTO.getEmail());
        }

        Customer newCustomer = customerMapper.toEntity(customerDTO);
        // loyaltyPoints mặc định là 0
        Customer savedCustomer = customerRepository.save(newCustomer);
        return customerMapper.toDto(savedCustomer);
    }

    /**
     * Cập nhật thông tin khách hàng (không cập nhật điểm)
     */
    @Transactional
    public CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO) {
        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));

        // Kiểm tra SĐT mới (nếu thay đổi)
        if (!existingCustomer.getPhone().equals(customerDTO.getPhone()) &&
                customerRepository.existsByPhone(customerDTO.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists: " + customerDTO.getPhone());
        }
        // Kiểm tra Email mới (nếu thay đổi và không rỗng)
        if (customerDTO.getEmail() != null && !customerDTO.getEmail().isEmpty() &&
                !existingCustomer.getEmail().equals(customerDTO.getEmail()) &&
                customerRepository.existsByEmail(customerDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + customerDTO.getEmail());
        }

        // Dùng mapper cập nhật (mapper đã ignore loyaltyPoints, createdAt, updatedAt)
        customerMapper.updateEntityFromDto(customerDTO, existingCustomer);
        Customer updatedCustomer = customerRepository.save(existingCustomer);
        return customerMapper.toDto(updatedCustomer);
    }

    /**
     * Xoá khách hàng
     */
    @Transactional
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new EntityNotFoundException("Customer not found with id: " + id);
        }
        // (Cần kiểm tra xem Customer có đang được liên kết với Order nào không trước khi xoá)
        // (Tạm thời cho phép xoá)
        customerRepository.deleteById(id);
    }

    // --- CÁC NGHIỆP VỤ LIÊN QUAN ĐIỂM THƯỞNG SẼ THÊM SAU ---
    // Ví dụ: addLoyaltyPoints(Long customerId, int pointsToAdd);
}