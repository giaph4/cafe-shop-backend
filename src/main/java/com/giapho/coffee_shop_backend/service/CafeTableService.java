package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.enums.TableStatus;

import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.dto.CafeTableRequest;
import com.giapho.coffee_shop_backend.dto.CafeTableResponse;
import com.giapho.coffee_shop_backend.mapper.CafeTableMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeTableService {

    private final CafeTableRepository cafeTableRepository;
    private final CafeTableMapper cafeTableMapper;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<CafeTableResponse> getAllTables() {
        List<CafeTable> tables = cafeTableRepository.findAll();
        return cafeTableMapper.entityListToResponseList(tables);
    }

    @Transactional(readOnly = true)
    public CafeTableResponse getTableById(Long id) {
        CafeTable table = cafeTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Table not found with id: " + id));
        // SỬA LỖI: Trả về DTO đã map thay vì null
        return cafeTableMapper.entityToResponse(table);
    }

    @Transactional
    public CafeTableResponse createTable(CafeTableRequest request) {
        if (cafeTableRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Table with name " + request.getName() + " already exists");
        }

        CafeTable newTable = cafeTableMapper.requestToEntity(request);

        CafeTable savedTable = cafeTableRepository.save(newTable);

        return cafeTableMapper.entityToResponse(savedTable);
    }

    @Transactional
    public CafeTableResponse updateTableInfo(Long id, CafeTableRequest request) {
        CafeTable existingTable = cafeTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Table not found with id: " + id));

        if (!existingTable.getName().equals(request.getName()) &&
                cafeTableRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Table name already exists: " + request.getName());
        }

        cafeTableMapper.updateEntityFromRequest(request, existingTable);

        CafeTable updatedTable = cafeTableRepository.save(existingTable);

        return cafeTableMapper.entityToResponse(updatedTable);
    }

    @Transactional
    public CafeTableResponse updateTableStatus(Long id, String status) {
        TableStatus newStatus = parseStatus(status);
        CafeTable table = cafeTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Table not found with id: " + id));

        table.setStatus(newStatus);

        CafeTable updatedTable = cafeTableRepository.save(table);

        return cafeTableMapper.entityToResponse(updatedTable);
    }

    @Transactional
    public void deleteTable(Long id) {
        CafeTable table = cafeTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Table not found with id: " + id));

        long orderCount = orderRepository.countByCafeTableId(id); // Giả sử bạn thêm hàm countByCafeTableId vào OrderRepository
        if (orderCount > 0) {
            throw new IllegalArgumentException("Cannot delete table '" + table.getName() + "' because it has associated orders. Please resolve or reassign the orders first.");
        }


        // 3. Nếu không có order nào, tiến hành xóa bàn
        cafeTableRepository.deleteById(id);
        System.out.println("Deleted table with ID: " + id); // Log (tùy chọn)
    }

    private TableStatus parseStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        try {
            return TableStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            String allowedValues = Arrays.stream(TableStatus.values())
                    .map(Enum::name)
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            throw new IllegalArgumentException("Invalid status. Must be one of: " + allowedValues, ex);
        }
    }
}