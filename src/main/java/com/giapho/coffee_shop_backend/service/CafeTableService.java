package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.dto.CafeTableRequest;
import com.giapho.coffee_shop_backend.dto.CafeTableResponse;
import com.giapho.coffee_shop_backend.mapper.CafeTableMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeTableService {

    private final CafeTableRepository cafeTableRepository;
    private final CafeTableMapper cafeTableMapper;

    /**
     * Lấy tất cả các bàn
     */
    @Transactional(readOnly = true)
    public List<CafeTableResponse> getAllTables() {
        List<CafeTable> tables = cafeTableRepository.findAll();
        return cafeTableMapper.entityListToResponseList(tables);
    }

    /**
     * Lấy chi tiết 1 bàn
     */
    @Transactional(readOnly = true)
    public CafeTableResponse getTableById(Long id) {
        CafeTable table = cafeTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Table not found with id: " + id));
        return null;
    }

    /**
     * Tạo bàn mới
     */
    @Transactional
    public CafeTableResponse createTable(CafeTableRequest request) {
        if (cafeTableRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Table with name " + request.getName() + " already exists");
        }

        CafeTable newTable = cafeTableMapper.requestToEntity(request);

        CafeTable savedTable = cafeTableRepository.save(newTable);

        return cafeTableMapper.entityToResponse(savedTable);
    }

    /**
     * Cập nhật thông tin bàn (tên, sức chứa)
     */
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

    /**
     * Cập nhật trạng thái bàn (NGHIỆP VỤ RIÊNG)
     */
    @Transactional
    public CafeTableResponse updateTableStatus(Long id, String status) {
        // (Có thể thêm logic kiểm tra status hợp lệ: "EMPTY", "SERVING", "RESERVED")
        if (status == null || (!status.equals("EMPTY") && !status.equals("SERVING") && !status.equals("RESERVED"))) {
            throw new IllegalArgumentException("Invalid status. Must be one of: EMPTY, SERVING, RESERVED");
        }

        CafeTable table = cafeTableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Table not found with id: " + id));

        table.setStatus(status);

        CafeTable updatedTable = cafeTableRepository.save(table);

        return cafeTableMapper.entityToResponse(updatedTable);
    }

    /**
     * Xoá bàn
     */
    @Transactional
    public void deleteTable(Long id) {
        if (!cafeTableRepository.existsById(id)) {
            throw new EntityNotFoundException("Table not found with id: " + id);
        }
        // (Cần kiểm tra xem bàn có đang được gán cho Order nào không trước khi xoá)
        // (Tạm thời chúng ta cho xoá)
        cafeTableRepository.deleteById(id);
    }
}
