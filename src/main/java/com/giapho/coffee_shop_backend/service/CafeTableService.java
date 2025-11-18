package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.CafeTableRequest;
import com.giapho.coffee_shop_backend.dto.CafeTableResponse;

import java.util.List;

public interface CafeTableService {

    List<CafeTableResponse> getAllTables();

    CafeTableResponse getTableById(Long id);

    CafeTableResponse createTable(CafeTableRequest request);

    CafeTableResponse updateTableInfo(Long id, CafeTableRequest request);

    CafeTableResponse updateTableStatus(Long id, String status);

    void deleteTable(Long id);
}
