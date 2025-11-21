package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.shift.AttendanceCheckRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.AttendanceRecordResponseDTO;

import java.util.List;

/**
 * Hợp đồng dịch vụ chấm công cho nhân sự.
 */
public interface AttendanceService {

    AttendanceRecordResponseDTO checkIn(AttendanceCheckRequestDTO request);

    AttendanceRecordResponseDTO checkOut(AttendanceCheckRequestDTO request);

    List<AttendanceRecordResponseDTO> getAttendanceForAssignment(Long assignmentId);

    List<AttendanceRecordResponseDTO> getAttendanceForShift(Long shiftId);
}
