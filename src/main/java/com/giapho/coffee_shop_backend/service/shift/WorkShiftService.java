package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.dto.shift.WorkShiftRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.WorkShiftResponseDTO;

import java.util.List;

public interface WorkShiftService {

    WorkShiftResponseDTO createWorkShift(WorkShiftRequestDTO request);

    WorkShiftResponseDTO updateWorkShift(Long id, WorkShiftRequestDTO request);

    WorkShiftResponseDTO updateStatus(Long id, boolean active);

    WorkShiftResponseDTO getWorkShift(Long id);

    List<WorkShiftResponseDTO> listActiveWorkShifts();
}
