package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.WorkShift;
import com.giapho.coffee_shop_backend.domain.repository.WorkShiftRepository;
import com.giapho.coffee_shop_backend.dto.shift.WorkShiftRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.WorkShiftResponseDTO;
import com.giapho.coffee_shop_backend.exception.shift.WorkShiftNotFoundException;
import com.giapho.coffee_shop_backend.mapper.WorkShiftMapper;
import com.giapho.coffee_shop_backend.service.shift.impl.WorkShiftServiceImpl;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkShiftServiceImplTest {

    @Mock
    private WorkShiftRepository workShiftRepository;
    @Mock
    private WorkShiftMapper workShiftMapper;

    @InjectMocks
    private WorkShiftServiceImpl workShiftService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentUsername).thenReturn(Optional.of("manager01"));
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    @Test
    void createWorkShift_ShouldPersistWithAuditMetadata() {
        WorkShiftRequestDTO request = new WorkShiftRequestDTO(
                "Ca sáng",
                "Ca từ 7h-12h",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(5),
                5,
                false
        );

        WorkShift transientShift = WorkShift.builder()
                .name("Ca sáng")
                .description("Ca từ 7h-12h")
                .startAt(request.startAt())
                .endAt(request.endAt())
                .maxEmployees(request.maxEmployees())
                .build();

        when(workShiftMapper.toEntity(request)).thenReturn(transientShift);
        when(workShiftRepository.save(any(WorkShift.class))).thenAnswer(invocation -> {
            WorkShift saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        WorkShiftResponseDTO expectedResponse = new WorkShiftResponseDTO(
                10L,
                "Ca sáng",
                "Ca từ 7h-12h",
                request.startAt(),
                request.endAt(),
                5,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(workShiftMapper.toResponseDTO(any(WorkShift.class))).thenReturn(expectedResponse);

        WorkShiftResponseDTO result = workShiftService.createWorkShift(request);

        assertThat(result).isEqualTo(expectedResponse);

        ArgumentCaptor<WorkShift> captor = ArgumentCaptor.forClass(WorkShift.class);
        verify(workShiftRepository).save(captor.capture());
        WorkShift savedEntity = captor.getValue();
        assertThat(savedEntity.isActive()).isFalse();
        assertThat(savedEntity.getCreatedBy()).isEqualTo("manager01");
        assertThat(savedEntity.getUpdatedBy()).isEqualTo("manager01");
        assertThat(savedEntity.getCreatedAt()).isNotNull();
    }

    @Test
    void updateWorkShift_ShouldApplyChangesAndAudit() {
        Long shiftId = 20L;
        WorkShiftRequestDTO request = new WorkShiftRequestDTO(
                "Ca chiều",
                "Cập nhật mô tả",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(4),
                8,
                true
        );

        WorkShift existing = WorkShift.builder()
                .id(shiftId)
                .name("Old name")
                .description("Old desc")
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(1))
                .maxEmployees(3)
                .isActive(false)
                .build();

        when(workShiftRepository.findById(shiftId)).thenReturn(Optional.of(existing));
        when(workShiftRepository.save(existing)).thenReturn(existing);
        when(workShiftMapper.toResponseDTO(existing)).thenReturn(new WorkShiftResponseDTO(
                shiftId,
                request.name(),
                request.description(),
                request.startAt(),
                request.endAt(),
                request.maxEmployees(),
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        ));

        WorkShiftResponseDTO response = workShiftService.updateWorkShift(shiftId, request);

        assertThat(response.name()).isEqualTo("Ca chiều");
        assertThat(existing.getUpdatedBy()).isEqualTo("manager01");
        assertThat(existing.isActive()).isTrue();
        verify(workShiftMapper).updateEntityFromRequest(request, existing);
        verify(workShiftRepository).save(existing);
    }

    @Test
    void updateWorkShift_ShouldThrow_WhenNotFound() {
        Long shiftId = 99L;
        when(workShiftRepository.findById(shiftId)).thenReturn(Optional.empty());

        assertThrows(WorkShiftNotFoundException.class,
                () -> workShiftService.updateWorkShift(shiftId, null));
    }

    @Test
    void updateStatus_ShouldSetActiveFlag() {
        Long shiftId = 30L;
        WorkShift existing = WorkShift.builder()
                .id(shiftId)
                .isActive(false)
                .build();
        when(workShiftRepository.findById(shiftId)).thenReturn(Optional.of(existing));
        when(workShiftRepository.save(existing)).thenReturn(existing);
        WorkShiftResponseDTO dto = new WorkShiftResponseDTO(
                shiftId, "name", null, null, null, null, true,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(workShiftMapper.toResponseDTO(existing)).thenReturn(dto);

        WorkShiftResponseDTO response = workShiftService.updateStatus(shiftId, true);

        assertThat(response.active()).isTrue();
        assertThat(existing.isActive()).isTrue();
        assertThat(existing.getUpdatedBy()).isEqualTo("manager01");
        verify(workShiftRepository).save(existing);
    }

    @Test
    void getWorkShift_ShouldReturnResponse() {
        Long shiftId = 40L;
        WorkShift entity = WorkShift.builder().id(shiftId).name("Ca tối").build();
        WorkShiftResponseDTO responseDTO = new WorkShiftResponseDTO(
                shiftId, "Ca tối", null, null, null, null, true,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(workShiftRepository.findById(shiftId)).thenReturn(Optional.of(entity));
        when(workShiftMapper.toResponseDTO(entity)).thenReturn(responseDTO);

        WorkShiftResponseDTO result = workShiftService.getWorkShift(shiftId);

        assertThat(result).isEqualTo(responseDTO);
    }

    @Test
    void listActiveWorkShifts_ShouldMapAllEntities() {
        WorkShift shift1 = WorkShift.builder().id(1L).name("A").build();
        WorkShift shift2 = WorkShift.builder().id(2L).name("B").build();
        when(workShiftRepository.findByIsActiveTrueOrderByStartAtAsc()).thenReturn(List.of(shift1, shift2));
        when(workShiftMapper.toResponseDTO(shift1)).thenReturn(new WorkShiftResponseDTO(
                1L, "A", null, null, null, null, true,
                LocalDateTime.now(), LocalDateTime.now()
        ));
        when(workShiftMapper.toResponseDTO(shift2)).thenReturn(new WorkShiftResponseDTO(
                2L, "B", null, null, null, null, true,
                LocalDateTime.now(), LocalDateTime.now()
        ));

        List<WorkShiftResponseDTO> result = workShiftService.listActiveWorkShifts();

        assertThat(result).hasSize(2);
        verify(workShiftRepository).findByIsActiveTrueOrderByStartAtAsc();
    }
}
