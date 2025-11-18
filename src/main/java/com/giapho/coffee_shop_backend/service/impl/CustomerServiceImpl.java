package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Customer;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.repository.CustomerPurchaseAggregate;
import com.giapho.coffee_shop_backend.domain.repository.CustomerRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.dto.CustomerDTO;
import com.giapho.coffee_shop_backend.dto.CustomerPurchaseHistoryItemDTO;
import com.giapho.coffee_shop_backend.dto.CustomerPurchaseHistoryResponseDTO;
import com.giapho.coffee_shop_backend.exception.customer.CustomerDeletionNotAllowedException;
import com.giapho.coffee_shop_backend.exception.customer.CustomerEmailAlreadyExistsException;
import com.giapho.coffee_shop_backend.exception.customer.CustomerNotFoundException;
import com.giapho.coffee_shop_backend.exception.customer.CustomerPhoneAlreadyExistsException;
import com.giapho.coffee_shop_backend.mapper.CustomerMapper;
import com.giapho.coffee_shop_backend.mapper.CustomerPurchaseHistoryMapper;
import com.giapho.coffee_shop_backend.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private static final BigDecimal FIRST_TIER_THRESHOLD = BigDecimal.valueOf(30_000);
    private static final BigDecimal SECOND_TIER_THRESHOLD = BigDecimal.valueOf(50_000);
    private static final BigDecimal THIRD_TIER_THRESHOLD = BigDecimal.valueOf(100_000);

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final CustomerMapper customerMapper;
    private final CustomerPurchaseHistoryMapper customerPurchaseHistoryMapper;

    @Override
    public Page<CustomerDTO> searchCustomers(String keyword, Pageable pageable) {
        Page<Customer> customers = StringUtils.hasText(keyword)
                ? customerRepository.findByFullNameContainingIgnoreCaseOrPhoneContaining(keyword, keyword, pageable)
                : customerRepository.findAll(pageable);
        return customers.map(customerMapper::toDto);
    }

    @Override
    public CustomerDTO getCustomerById(Long id) {
        Customer customer = findCustomerById(id);
        return customerMapper.toDto(customer);
    }

    @Override
    @Transactional
    public void updateLoyaltyPoints(Long customerId, BigDecimal totalAmount) {
        if (customerId == null || totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        int pointsToAdd = calculatePoints(totalAmount);
        if (pointsToAdd <= 0) {
            return;
        }

        Customer customer = findCustomerById(customerId);
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsToAdd);
        customerRepository.save(customer);

        log.info("Added {} loyalty points (amount: {}) for customer {}. Total points: {}",
                pointsToAdd, totalAmount, customerId, customer.getLoyaltyPoints());
    }

    @Override
    public CustomerDTO getCustomerByPhone(String phone) {
        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new CustomerNotFoundException("phone", phone));
        return customerMapper.toDto(customer);
    }

    @Override
    @Transactional
    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        validateUniquePhone(customerDTO.getPhone(), null);
        validateUniqueEmail(customerDTO.getEmail(), null);

        Customer newCustomer = customerMapper.toEntity(customerDTO);
        Customer savedCustomer = customerRepository.save(newCustomer);
        log.info("Created customer {} with phone {}", savedCustomer.getId(), savedCustomer.getPhone());
        return customerMapper.toDto(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO) {
        Customer existingCustomer = findCustomerById(id);

        validateUniquePhone(customerDTO.getPhone(), id);
        validateUniqueEmail(customerDTO.getEmail(), id);

        customerMapper.updateEntityFromDto(customerDTO, existingCustomer);
        Customer updatedCustomer = customerRepository.save(existingCustomer);
        log.info("Updated customer {}", id);
        return customerMapper.toDto(updatedCustomer);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException(id);
        }

        ensureCustomerHasNoOrders(id);
        customerRepository.deleteById(id);
        log.info("Deleted customer {}", id);
    }

    @Override
    public CustomerPurchaseHistoryResponseDTO getCustomerPurchaseHistory(Long customerId,
                                                                          LocalDate startDate,
                                                                          LocalDate endDate,
                                                                          String status,
                                                                          Pageable pageable) {
        Customer customer = findCustomerById(customerId);

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay().minusNanos(1) : null;
        String normalizedStatus = normalizeStatus(status);

        Page<Long> orderIdPage = orderRepository.findCustomerOrderIds(
                customerId,
                normalizedStatus,
                startDateTime,
                endDateTime,
                pageable
        );

        List<CustomerPurchaseHistoryItemDTO> historyItems = orderIdPage.isEmpty()
                ? Collections.emptyList()
                : buildHistoryItems(orderIdPage.getContent());

        CustomerPurchaseAggregate aggregate = orderRepository.calculateCustomerPurchaseAggregate(
                customerId,
                normalizedStatus,
                startDateTime,
                endDateTime
        );

        return customerPurchaseHistoryMapper.aggregateToResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                aggregate,
                historyItems,
                orderIdPage.getNumber(),
                orderIdPage.getSize(),
                orderIdPage.getTotalElements(),
                orderIdPage.getTotalPages(),
                orderIdPage.hasNext(),
                orderIdPage.hasPrevious()
        );
    }

    private Customer findCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private int calculatePoints(BigDecimal totalAmount) {
        if (totalAmount.compareTo(THIRD_TIER_THRESHOLD) >= 0) {
            return 50;
        }
        if (totalAmount.compareTo(SECOND_TIER_THRESHOLD) >= 0) {
            return 20;
        }
        if (totalAmount.compareTo(FIRST_TIER_THRESHOLD) >= 0) {
            return 10;
        }
        return 0;
    }

    private void validateUniquePhone(String phone, Long currentCustomerId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        boolean exists = customerRepository.existsByPhone(phone);
        if (!exists) {
            return;
        }
        if (currentCustomerId == null) {
            throw new CustomerPhoneAlreadyExistsException(phone);
        }
        Customer current = findCustomerById(currentCustomerId);
        if (!phone.equals(current.getPhone())) {
            throw new CustomerPhoneAlreadyExistsException(phone);
        }
    }

    private void validateUniqueEmail(String email, Long currentCustomerId) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        boolean exists = customerRepository.existsByEmail(email);
        if (!exists) {
            return;
        }
        if (currentCustomerId == null) {
            throw new CustomerEmailAlreadyExistsException(email);
        }
        Customer current = findCustomerById(currentCustomerId);
        if (!email.equals(current.getEmail())) {
            throw new CustomerEmailAlreadyExistsException(email);
        }
    }

    private void ensureCustomerHasNoOrders(Long customerId) {
        Pageable singleResult = PageRequest.of(0, 1);
        Page<Long> orderIds = orderRepository.findCustomerOrderIds(customerId, null, null, null, singleResult);
        if (!orderIds.isEmpty()) {
            throw new CustomerDeletionNotAllowedException(customerId);
        }
    }

    private List<CustomerPurchaseHistoryItemDTO> buildHistoryItems(List<Long> orderIds) {
        Map<Long, Order> orderMap = fetchOrders(orderIds);
        return orderIds.stream()
                .map(orderMap::get)
                .filter(order -> order != null)
                .map(customerPurchaseHistoryMapper::orderToHistoryItem)
                .toList();
    }

    private Map<Long, Order> fetchOrders(List<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderRepository.findCustomerOrdersByIds(orderIds).stream()
                .collect(Collectors.toMap(
                        Order::getId,
                        Function.identity(),
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return status.trim().toUpperCase();
    }
}
