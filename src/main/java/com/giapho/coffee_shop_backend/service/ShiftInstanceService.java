package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceCreateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceStatusUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ShiftInstanceService {

    Page<ShiftInstanceResponseDTO> listInstances(LocalDate from, LocalDate to, ShiftStatus status, Pageable pageable);

    ShiftInstanceResponseDTO getInstance(Long id);

    List<ShiftInstanceResponseDTO> createInstances(ShiftInstanceCreateRequestDTO request);

    ShiftInstanceResponseDTO updateInstance(Long id, ShiftInstanceCreateRequestDTO request);

    ShiftInstanceResponseDTO updateStatus(Long id, ShiftInstanceStatusUpdateRequestDTO request);

    void deleteInstance(Long id);
}
