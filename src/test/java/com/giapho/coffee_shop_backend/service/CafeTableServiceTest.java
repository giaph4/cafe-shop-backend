package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.enums.TableStatus;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.dto.CafeTableRequest;
import com.giapho.coffee_shop_backend.dto.CafeTableResponse;
import com.giapho.coffee_shop_backend.exception.cafetable.CafeTableConflictException;
import com.giapho.coffee_shop_backend.exception.cafetable.CafeTableDeletionNotAllowedException;
import com.giapho.coffee_shop_backend.mapper.CafeTableMapper;
import com.giapho.coffee_shop_backend.service.cafetable.helper.CafeTableValidator;
import com.giapho.coffee_shop_backend.service.impl.CafeTableServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CafeTableServiceTest {

    @Mock
    private CafeTableRepository cafeTableRepository;
    @Mock
    private CafeTableMapper cafeTableMapper;
    @Mock
    private CafeTableValidator cafeTableValidator;

    @InjectMocks
    private CafeTableServiceImpl cafeTableService;

    private CafeTableRequest baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = new CafeTableRequest();
        baseRequest.setName(" Bàn 01 ");
        baseRequest.setCapacity(4);
    }

    @Test
    void getAllTables_shouldReturnMappedList() {
        List<CafeTable> entities = List.of(buildTable(1L, "Ban 1", TableStatus.EMPTY));
        List<CafeTableResponse> responses = List.of(buildResponse(1L, "Ban 1", TableStatus.EMPTY.name()));

        when(cafeTableRepository.findAll()).thenReturn(entities);
        when(cafeTableMapper.entityListToResponseList(entities)).thenReturn(responses);

        List<CafeTableResponse> result = cafeTableService.getAllTables();

        assertThat(result).isEqualTo(responses);
        verify(cafeTableRepository).findAll();
        verify(cafeTableMapper).entityListToResponseList(entities);
    }

    @Test
    void getTableById_shouldReturnMappedResponse() {
        CafeTable entity = buildTable(5L, "Ban 5", TableStatus.SERVING);
        CafeTableResponse response = buildResponse(5L, "Ban 5", TableStatus.SERVING.name());

        when(cafeTableValidator.requireExistingTable(5L)).thenReturn(entity);
        when(cafeTableMapper.entityToResponse(entity)).thenReturn(response);

        CafeTableResponse result = cafeTableService.getTableById(5L);

        assertThat(result).isEqualTo(response);
        verify(cafeTableValidator).requireExistingTable(5L);
        verify(cafeTableMapper).entityToResponse(entity);
    }

    @Test
    void createTable_shouldNormalizeNameSetStatusAndPersist() {
        CafeTable mappedEntity = buildTable(null, "Không dùng", TableStatus.AVAILABLE);
        CafeTable persisted = buildTable(10L, "Ban 1", TableStatus.EMPTY);
        CafeTableResponse response = buildResponse(10L, "Ban 1", TableStatus.EMPTY.name());

        when(cafeTableValidator.normalizeName(" Bàn 01 ")).thenReturn("Ban 1");
        when(cafeTableMapper.requestToEntity(baseRequest)).thenReturn(mappedEntity);
        when(cafeTableRepository.save(any(CafeTable.class))).thenReturn(persisted);
        when(cafeTableMapper.entityToResponse(persisted)).thenReturn(response);

        CafeTableResponse result = cafeTableService.createTable(baseRequest);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<CafeTable> captor = ArgumentCaptor.forClass(CafeTable.class);
        verify(cafeTableRepository).save(captor.capture());
        CafeTable saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Ban 1");
        assertThat(saved.getStatus()).isEqualTo(TableStatus.EMPTY);

        verify(cafeTableValidator).ensureNameUnique("Ban 1", null);
    }

    @Test
    void createTable_shouldPropagateConflictWhenNameExists() {
        when(cafeTableValidator.normalizeName(" Bàn 01 ")).thenReturn("Ban 1");
        doThrow(new CafeTableConflictException("Ban 1"))
                .when(cafeTableValidator).ensureNameUnique("Ban 1", null);

        assertThatThrownBy(() -> cafeTableService.createTable(baseRequest))
                .isInstanceOf(CafeTableConflictException.class)
                .hasMessageContaining("Ban 1");

        verify(cafeTableRepository, never()).save(any());
    }

    @Test
    void updateTableInfo_shouldValidateAndPersist() {
        CafeTable existing = buildTable(3L, "Ban 3", TableStatus.EMPTY);
        CafeTableResponse response = buildResponse(3L, "Ban 01", TableStatus.EMPTY.name());

        when(cafeTableValidator.requireExistingTable(3L)).thenReturn(existing);
        when(cafeTableValidator.normalizeName(" Bàn 01 ")).thenReturn("Ban 01");
        when(cafeTableRepository.save(existing)).thenReturn(existing);
        when(cafeTableMapper.entityToResponse(existing)).thenReturn(response);

        CafeTableResponse result = cafeTableService.updateTableInfo(3L, baseRequest);

        assertThat(result).isEqualTo(response);
        assertThat(existing.getName()).isEqualTo("Ban 01");
        verify(cafeTableValidator).ensureNameUnique("Ban 01", 3L);
        verify(cafeTableMapper).updateEntityFromRequest(baseRequest, existing);
    }

    @Test
    void updateTableStatus_shouldUseValidatorAndRepository() {
        CafeTable existing = buildTable(7L, "Ban 7", TableStatus.EMPTY);
        CafeTableResponse response = buildResponse(7L, "Ban 7", TableStatus.RESERVED.name());

        when(cafeTableValidator.requireExistingTable(7L)).thenReturn(existing);
        when(cafeTableValidator.parseStatus("RESERVED")).thenReturn(TableStatus.RESERVED);
        when(cafeTableRepository.save(existing)).thenReturn(existing);
        when(cafeTableMapper.entityToResponse(existing)).thenReturn(response);

        CafeTableResponse result = cafeTableService.updateTableStatus(7L, "RESERVED");

        assertThat(result).isEqualTo(response);
        assertThat(existing.getStatus()).isEqualTo(TableStatus.RESERVED);
        verify(cafeTableValidator).parseStatus("RESERVED");
        verify(cafeTableRepository).save(existing);
    }

    @Test
    void deleteTable_shouldValidateAndDelete() {
        CafeTable existing = buildTable(9L, "Ban 9", TableStatus.EMPTY);
        when(cafeTableValidator.requireExistingTable(9L)).thenReturn(existing);

        cafeTableService.deleteTable(9L);

        verify(cafeTableValidator).ensureTableDeletable(9L);
        verify(cafeTableRepository).delete(existing);
    }

    @Test
    void deleteTable_shouldPropagateValidatorException() {
        CafeTable existing = buildTable(11L, "Ban 11", TableStatus.SERVING);
        when(cafeTableValidator.requireExistingTable(11L)).thenReturn(existing);
        doThrow(new CafeTableDeletionNotAllowedException(11L))
                .when(cafeTableValidator).ensureTableDeletable(11L);

        assertThatThrownBy(() -> cafeTableService.deleteTable(11L))
                .isInstanceOf(CafeTableDeletionNotAllowedException.class);

        verify(cafeTableRepository, never()).delete(any());
    }

    private CafeTable buildTable(Long id, String name, TableStatus status) {
        CafeTable table = new CafeTable();
        table.setId(id);
        table.setName(name);
        table.setCapacity(4);
        table.setStatus(status);
        return table;
    }

    private CafeTableResponse buildResponse(Long id, String name, String status) {
        CafeTableResponse response = new CafeTableResponse();
        response.setId(id);
        response.setName(name);
        response.setCapacity(4);
        response.setStatus(status);
        return response;
    }
}
