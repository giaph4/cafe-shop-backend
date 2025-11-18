package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.CustomerDTO;
import com.giapho.coffee_shop_backend.dto.CustomerPurchaseHistoryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CustomerService {

    Page<CustomerDTO> searchCustomers(String keyword, Pageable pageable);

    CustomerDTO getCustomerById(Long id);

    void updateLoyaltyPoints(Long customerId, BigDecimal totalAmount);

    CustomerDTO getCustomerByPhone(String phone);

    CustomerDTO createCustomer(CustomerDTO customerDTO);

    CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO);

    void deleteCustomer(Long id);

    CustomerPurchaseHistoryResponseDTO getCustomerPurchaseHistory(Long customerId,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  String status,
                                                                  Pageable pageable);
}