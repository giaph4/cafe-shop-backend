package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.CafeTableRequest;
import com.giapho.coffee_shop_backend.dto.CafeTableResponse;
import com.giapho.coffee_shop_backend.service.CafeTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
public class CafeTableController {

    private final CafeTableService cafeTableService;

    /**
     * API to get all tables in the coffee shop
     * @return a list of table responses
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<CafeTableResponse>> getAllTables() {
        List<CafeTableResponse> tables = cafeTableService.getAllTables();
        return ResponseEntity.ok(tables);
    }

    /**
     * API to get a table by its ID
     * @param id the ID of the table to be retrieved
     * @return a table response object
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CafeTableResponse> getTableById(@PathVariable Long id) {
        CafeTableResponse table = cafeTableService.getTableById(id);
        return ResponseEntity.ok(table);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<CafeTableResponse> createTable(@Valid @RequestBody CafeTableRequest request) {
        CafeTableResponse createdTable = cafeTableService.createTable(request);
        return new ResponseEntity<>(createdTable, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<CafeTableResponse> updateTableInfo(
            @PathVariable Long id,
            @Valid @RequestBody CafeTableRequest request
    ) {
        CafeTableResponse updatedTable = cafeTableService.updateTableInfo(id, request);
        return ResponseEntity.ok(updatedTable);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CafeTableResponse> updateTableStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusMap
    ) {
        String status = statusMap.get("status");
        CafeTableResponse updatedTable = cafeTableService.updateTableStatus(id, status);
        return ResponseEntity.ok(updatedTable);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteTable(@PathVariable Long id) {
        cafeTableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }
}
