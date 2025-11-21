package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.shift.AttendanceCheckRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.AttendanceRecordResponseDTO;
import com.giapho.coffee_shop_backend.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public ResponseEntity<AttendanceRecordResponseDTO> checkIn(
            @Valid @RequestBody AttendanceCheckRequestDTO request
    ) {
        AttendanceRecordResponseDTO response = attendanceService.checkIn(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public ResponseEntity<AttendanceRecordResponseDTO> checkOut(
            @Valid @RequestBody AttendanceCheckRequestDTO request
    ) {
        AttendanceRecordResponseDTO response = attendanceService.checkOut(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<AttendanceRecordResponseDTO>> getAttendanceForAssignment(
            @PathVariable Long assignmentId
    ) {
        List<AttendanceRecordResponseDTO> records = attendanceService.getAttendanceForAssignment(assignmentId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/shift/{shiftId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<AttendanceRecordResponseDTO>> getAttendanceForShift(
            @PathVariable Long shiftId
    ) {
        List<AttendanceRecordResponseDTO> records = attendanceService.getAttendanceForShift(shiftId);
        return ResponseEntity.ok(records);
    }
}
