package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.shift.ShiftSessionResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftSessionStartRequestDTO;
import com.giapho.coffee_shop_backend.service.shift.ShiftSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts/sessions")
@RequiredArgsConstructor
@Validated
@Tag(name = "Shift Session Management", description = "APIs for managing shift sessions")
public class ShiftSessionController {

    private final ShiftSessionService shiftSessionService;

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Bắt đầu ca làm việc", description = "Tạo mới một phiên làm việc cho nhân viên")
    public ResponseEntity<ShiftSessionResponseDTO> startSession(
            @Valid @RequestBody ShiftSessionStartRequestDTO request
    ) {
        ShiftSessionResponseDTO response = shiftSessionService.startSession(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/end")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Kết thúc ca làm việc", description = "Kết thúc phiên làm việc hiện tại của nhân viên")
    public ResponseEntity<ShiftSessionResponseDTO> endSession() {
        ShiftSessionResponseDTO response = shiftSessionService.endCurrentSession();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Xem ca làm việc hiện tại", description = "Xem thông tin phiên làm việc hiện tại của nhân viên")
    public ResponseEntity<ShiftSessionResponseDTO> getCurrentSession() {
        ShiftSessionResponseDTO response = shiftSessionService.getCurrentActiveSession();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/force")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Kết thúc cưỡng bức ca làm việc", 
              description = "Quản lý hoặc admin có thể kết thúc cưỡng bức một phiên làm việc")
    public ResponseEntity<ShiftSessionResponseDTO> forceEndSession(
            @PathVariable @Min(1) Long id,
            @RequestParam(required = false) String reason
    ) {
        ShiftSessionResponseDTO response = shiftSessionService.forceEndSession(id, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shift/{shiftId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Xem danh sách ca làm việc đang hoạt động", 
              description = "Xem danh sách các phiên làm việc đang hoạt động theo ca")
    public ResponseEntity<List<ShiftSessionResponseDTO>> listActiveSessionsByShift(
            @PathVariable @Min(1) Long shiftId
    ) {
        List<ShiftSessionResponseDTO> response = shiftSessionService.listActiveSessionsByShift(shiftId);
        return ResponseEntity.ok(response);
    }
}
