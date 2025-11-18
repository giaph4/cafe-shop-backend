package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.enums.TableStatus;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.dto.CafeTableRequest;
import com.giapho.coffee_shop_backend.dto.CafeTableResponse;
import com.giapho.coffee_shop_backend.mapper.CafeTableMapper;
import com.giapho.coffee_shop_backend.service.CafeTableService;
import com.giapho.coffee_shop_backend.service.cafetable.helper.CafeTableValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Triển khai nghiệp vụ chính cho module quản lý bàn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CafeTableServiceImpl implements CafeTableService {

    private final CafeTableRepository cafeTableRepository;
    private final CafeTableMapper cafeTableMapper;
    private final CafeTableValidator cafeTableValidator;

    @Override
    @Transactional(readOnly = true)
    public List<CafeTableResponse> getAllTables() {
        return cafeTableMapper.entityListToResponseList(cafeTableRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public CafeTableResponse getTableById(Long id) {
        CafeTable table = cafeTableValidator.requireExistingTable(id);
        return cafeTableMapper.entityToResponse(table);
    }

    @Override
    public CafeTableResponse createTable(CafeTableRequest request) {
        String normalizedName = cafeTableValidator.normalizeName(request.getName());
        cafeTableValidator.ensureNameUnique(normalizedName, null);

        CafeTable newTable = cafeTableMapper.requestToEntity(request);
        newTable.setName(normalizedName);
        newTable.setStatus(TableStatus.EMPTY);

        CafeTable savedTable = cafeTableRepository.save(newTable);
        log.info("Created cafe table {} with name {}", savedTable.getId(), savedTable.getName());
        return cafeTableMapper.entityToResponse(savedTable);
    }

    @Override
    public CafeTableResponse updateTableInfo(Long id, CafeTableRequest request) {
        CafeTable existingTable = cafeTableValidator.requireExistingTable(id);

        String normalizedName = cafeTableValidator.normalizeName(request.getName());
        cafeTableValidator.ensureNameUnique(normalizedName, existingTable.getId());

        cafeTableMapper.updateEntityFromRequest(request, existingTable);
        existingTable.setName(normalizedName);

        CafeTable updatedTable = cafeTableRepository.save(existingTable);
        log.info("Updated cafe table {}", updatedTable.getId());
        return cafeTableMapper.entityToResponse(updatedTable);
    }

    @Override
    public CafeTableResponse updateTableStatus(Long id, String status) {
        CafeTable existingTable = cafeTableValidator.requireExistingTable(id);
        TableStatus newStatus = cafeTableValidator.parseStatus(status);

        existingTable.setStatus(newStatus);
        CafeTable updatedTable = cafeTableRepository.save(existingTable);

        log.info("Updated status of cafe table {} to {}", updatedTable.getId(), updatedTable.getStatus());
        return cafeTableMapper.entityToResponse(updatedTable);
    }

    @Override
    public void deleteTable(Long id) {
        CafeTable table = cafeTableValidator.requireExistingTable(id);
        cafeTableValidator.ensureTableDeletable(id);

        cafeTableRepository.delete(table);
        log.info("Deleted cafe table {}", id);
    }
}
