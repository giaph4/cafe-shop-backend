package com.giapho.coffee_shop_backend.service.shift.impl;

import com.giapho.coffee_shop_backend.domain.entity.WorkShift;
import com.giapho.coffee_shop_backend.domain.repository.WorkShiftRepository;
import com.giapho.coffee_shop_backend.dto.shift.WorkShiftRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.WorkShiftResponseDTO;
import com.giapho.coffee_shop_backend.exception.shift.WorkShiftNotFoundException;
import com.giapho.coffee_shop_backend.mapper.WorkShiftMapper;
import com.giapho.coffee_shop_backend.service.shift.WorkShiftService;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkShiftServiceImpl implements WorkShiftService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final WorkShiftRepository workShiftRepository;
    private final WorkShiftMapper workShiftMapper;

    @Override
    public WorkShiftResponseDTO createWorkShift(WorkShiftRequestDTO request) {
        WorkShift workShift = workShiftMapper.toEntity(request);
        if (request.active() != null) {
            workShift.setActive(request.active());
        }
        workShift.markCreated(resolveActor());
        WorkShift saved = workShiftRepository.save(workShift);
        return workShiftMapper.toResponseDTO(saved);
    }

    @Override
    public WorkShiftResponseDTO updateWorkShift(Long id, WorkShiftRequestDTO request) {
        WorkShift workShift = workShiftRepository.findById(id)
                .orElseThrow(() -> new WorkShiftNotFoundException(id));
        workShiftMapper.updateEntityFromRequest(request, workShift);
        if (request.active() != null) {
            workShift.setActive(request.active());
        }
        workShift.updateMetadata(resolveActor());
        WorkShift saved = workShiftRepository.save(workShift);
        return workShiftMapper.toResponseDTO(saved);
    }

    @Override
    public WorkShiftResponseDTO updateStatus(Long id, boolean active) {
        WorkShift workShift = workShiftRepository.findById(id)
                .orElseThrow(() -> new WorkShiftNotFoundException(id));
        workShift.setActive(active);
        workShift.updateMetadata(resolveActor());
        WorkShift saved = workShiftRepository.save(workShift);
        return workShiftMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkShiftResponseDTO getWorkShift(Long id) {
        return workShiftRepository.findById(id)
                .map(workShiftMapper::toResponseDTO)
                .orElseThrow(() -> new WorkShiftNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkShiftResponseDTO> listActiveWorkShifts() {
        return workShiftRepository.findByIsActiveTrueOrderByStartAtAsc().stream()
                .map(workShiftMapper::toResponseDTO)
                .toList();
    }

    private String resolveActor() {
        return SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
    }
}
