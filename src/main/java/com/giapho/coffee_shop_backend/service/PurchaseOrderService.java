package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.PurchaseOrderRequestDTO;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PurchaseOrderService {

    PurchaseOrderResponseDTO createPurchaseOrder(PurchaseOrderRequestDTO request);

    Page<PurchaseOrderResponseDTO> getAllPurchaseOrders(String status,
                                                        Long supplierId,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        Pageable pageable);

    PurchaseOrderResponseDTO getPurchaseOrderById(Long id);

    PurchaseOrderResponseDTO markPurchaseOrderAsCompleted(Long id);

    PurchaseOrderResponseDTO cancelPurchaseOrder(Long id);
}
