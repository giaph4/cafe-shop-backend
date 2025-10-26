package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.*;
import com.giapho.coffee_shop_backend.domain.repository.*;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderDetailRequestDTO;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderRequestDTO;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderResponseDTO;
import com.giapho.coffee_shop_backend.mapper.PurchaseOrderDetailMapper; // Cần mapper chi tiết
import com.giapho.coffee_shop_backend.mapper.PurchaseOrderMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderDetailMapper purchaseOrderDetailMapper;

    /**
     * Lấy danh sách phiếu nhập hàng (có phân trang)
     */
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponseDTO> getAllPurchaseOrders(Pageable pageable) {
        Page<PurchaseOrder> poPage = purchaseOrderRepository.findAll(pageable);
        return poPage.map(purchaseOrderMapper::entityToResponse);
    }

    /**
     * Lấy chi tiết một phiếu nhập hàng
     */
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDTO getPurchaseOrderById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase Order not found with id: " + id));
        return purchaseOrderMapper.entityToResponse(po);
    }

    /**
     * Tạo phiếu nhập hàng mới
     */
    @Transactional
    public PurchaseOrderResponseDTO createPurchaseOrder(PurchaseOrderRequestDTO request) {
        // 1. Lấy thông tin User đang đăng nhập
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        // 2. Lấy thông tin Nhà cung cấp
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + request.getSupplierId()));

        // 3. Tạo đối tượng PurchaseOrder ban đầu
        PurchaseOrder newPurchaseOrder = PurchaseOrder.builder()
                .supplier(supplier)
                .user(currentUser)
                .expectedDate(request.getExpectedDate())
                .status("PENDING") // Mặc định
                .totalAmount(BigDecimal.ZERO) // Sẽ tính sau
                .purchaseOrderDetails(new HashSet<>()) // Khởi tạo Set rỗng
                .build();

        // 4. Xử lý từng chi tiết nhập hàng
        Set<PurchaseOrderDetail> details = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderDetailRequestDTO itemDTO : request.getItems()) {
            // Lấy Ingredient từ DB (không cần kiểm tra isAvailable ở đây)
            Ingredient ingredient = ingredientRepository.findById(itemDTO.getIngredientId())
                    .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + itemDTO.getIngredientId()));

            // Tạo PurchaseOrderDetail
            PurchaseOrderDetail detail = PurchaseOrderDetail.builder()
                    .purchaseOrder(newPurchaseOrder) // Gán PO cha
                    .ingredient(ingredient)
                    .quantity(itemDTO.getQuantity())
                    .unitPrice(itemDTO.getUnitPrice())
                    .build();
            details.add(detail);

            // Cộng dồn vào tổng tiền
            totalAmount = totalAmount.add(itemDTO.getQuantity().multiply(itemDTO.getUnitPrice()));
        }

        // 5. Cập nhật lại PurchaseOrder với chi tiết và tổng tiền
        newPurchaseOrder.setPurchaseOrderDetails(details);
        newPurchaseOrder.setTotalAmount(totalAmount);

        // 6. Lưu PurchaseOrder (Cascade sẽ lưu Details)
        PurchaseOrder savedPO = purchaseOrderRepository.save(newPurchaseOrder);

        // 7. Trả về DTO (Fetch lại để đảm bảo EAGER loading)
        PurchaseOrder fetchedPO = purchaseOrderRepository.findById(savedPO.getId())
                .orElseThrow(() -> new EntityNotFoundException("Failed to fetch newly created purchase order"));
        return purchaseOrderMapper.entityToResponse(fetchedPO);
    }

    /**
     * Đánh dấu phiếu nhập hàng là HOÀN THÀNH (COMPLETED)
     * Đây là lúc cập nhật tồn kho nguyên vật liệu.
     */
    @Transactional
    public PurchaseOrderResponseDTO markPurchaseOrderAsCompleted(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase Order not found with id: " + id));

        if (!"PENDING".equals(po.getStatus())) {
            throw new IllegalArgumentException("Purchase Order is not in PENDING status. Current status: " + po.getStatus());
        }

        // Cập nhật trạng thái
        po.setStatus("COMPLETED");
        // Có thể thêm ngày nhận hàng thực tế nếu cần: po.setReceivedDate(LocalDateTime.now());

        // **Cập nhật tồn kho cho từng Ingredient**
        for (PurchaseOrderDetail detail : po.getPurchaseOrderDetails()) {
            Ingredient ingredient = detail.getIngredient();
            BigDecimal currentStock = ingredient.getQuantityOnHand();
            BigDecimal receivedQuantity = detail.getQuantity();
            ingredient.setQuantityOnHand(currentStock.add(receivedQuantity));
            // Không cần gọi ingredientRepository.save() vì Ingredient là managed entity,
            // Hibernate sẽ tự động cập nhật khi transaction kết thúc.
        }

        PurchaseOrder completedPO = purchaseOrderRepository.save(po); // Lưu lại trạng thái PO
        return purchaseOrderMapper.entityToResponse(completedPO);
    }

    /**
     * Huỷ một phiếu nhập hàng đang PENDING
     */
    @Transactional
    public PurchaseOrderResponseDTO cancelPurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase Order not found with id: " + id));

        if (!"PENDING".equals(po.getStatus())) {
            throw new IllegalArgumentException("Cannot cancel Purchase Order that is not PENDING. Current status: " + po.getStatus());
        }

        po.setStatus("CANCELLED");
        PurchaseOrder cancelledPO = purchaseOrderRepository.save(po);
        return purchaseOrderMapper.entityToResponse(cancelledPO);
    }

}