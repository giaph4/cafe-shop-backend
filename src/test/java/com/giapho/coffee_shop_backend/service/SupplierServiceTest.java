package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Supplier;
import com.giapho.coffee_shop_backend.domain.repository.SupplierRepository;
import com.giapho.coffee_shop_backend.dto.SupplierDTO;
import com.giapho.coffee_shop_backend.exception.supplier.SupplierConflictException;
import com.giapho.coffee_shop_backend.mapper.SupplierMapper;
import com.giapho.coffee_shop_backend.service.impl.SupplierServiceImpl;
import com.giapho.coffee_shop_backend.service.supplier.helper.SupplierValidator;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private SupplierMapper supplierMapper;
    @Mock
    private SupplierValidator supplierValidator;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private SupplierDTO baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = buildRequest("   ACME   ", " 0909 123 456 ");
    }

    @Test
    void getAllSuppliers_shouldReturnMappedList() {
        List<Supplier> entities = List.of(buildSupplier(1L, "ACME", "0909123456"));
        List<SupplierDTO> responses = List.of(buildResponse(1L, "ACME", "0909123456"));

        when(supplierRepository.findAll()).thenReturn(entities);
        when(supplierMapper.toDtoList(entities)).thenReturn(responses);

        List<SupplierDTO> result = supplierService.getAllSuppliers();

        assertThat(result).isEqualTo(responses);
        verify(supplierRepository).findAll();
        verify(supplierMapper).toDtoList(entities);
    }

    @Test
    void getSupplierById_shouldReturnMappedResponse() {
        Supplier entity = buildSupplier(5L, "ACME", "0909123456");
        SupplierDTO response = buildResponse(5L, "ACME", "0909123456");

        when(supplierValidator.requireExistingSupplier(5L)).thenReturn(entity);
        when(supplierMapper.toDto(entity)).thenReturn(response);

        SupplierDTO result = supplierService.getSupplierById(5L);

        assertThat(result).isEqualTo(response);
        verify(supplierValidator).requireExistingSupplier(5L);
        verify(supplierMapper).toDto(entity);
    }

    @Test
    void createSupplier_shouldNormalizeEnsureUniqueAndPersist() {
        Supplier mappedEntity = buildSupplier(null, "OLD", "000");
        Supplier persisted = buildSupplier(10L, "ACME", "0909123456");
        SupplierDTO response = buildResponse(10L, "ACME", "0909123456");

        when(supplierValidator.normalizeName("   ACME   ")).thenReturn("ACME");
        when(supplierValidator.normalizePhone(" 0909 123 456 ")).thenReturn("0909123456");
        when(supplierMapper.toEntity(baseRequest)).thenReturn(mappedEntity);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(persisted);
        when(supplierMapper.toDto(persisted)).thenReturn(response);

        SupplierDTO result = supplierService.createSupplier(baseRequest);

        assertThat(result).isEqualTo(response);
        verify(supplierValidator).ensureNameUnique("ACME", null);
        verify(supplierValidator).ensurePhoneUnique("0909123456", null);

        ArgumentCaptor<Supplier> entityCaptor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(entityCaptor.capture());
        Supplier saved = entityCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("ACME");
        assertThat(saved.getPhone()).isEqualTo("0909123456");
    }

    @Test
    void createSupplier_shouldPropagateConflictWhenValidatorFails() {
        when(supplierValidator.normalizeName("   ACME   ")).thenReturn("ACME");
        when(supplierValidator.normalizePhone(" 0909 123 456 ")).thenReturn("0909123456");
        doThrow(new SupplierConflictException("duplicated"))
                .when(supplierValidator).ensureNameUnique("ACME", null);

        assertThatThrownBy(() -> supplierService.createSupplier(baseRequest))
                .isInstanceOf(SupplierConflictException.class);

        verify(supplierRepository, never()).save(any());
    }

    @Test
    void updateSupplier_shouldRevalidateWhenFieldsChanged() {
        Supplier existing = buildSupplier(7L, "ACME", "0909123456");
        SupplierDTO response = buildResponse(7L, "NewCo", "0909999999");

        when(supplierValidator.requireExistingSupplier(7L)).thenReturn(existing);
        when(supplierValidator.normalizeName("   ACME   ")).thenReturn("NewCo");
        when(supplierValidator.normalizePhone(" 0909 123 456 ")).thenReturn("0909999999");
        when(supplierRepository.save(existing)).thenReturn(existing);
        when(supplierMapper.toDto(existing)).thenReturn(response);

        SupplierDTO result = supplierService.updateSupplier(7L, baseRequest);

        assertThat(result).isEqualTo(response);
        verify(supplierValidator).ensureNameUnique("NewCo", 7L);
        verify(supplierValidator).ensurePhoneUnique("0909999999", 7L);
        verify(supplierMapper).updateEntityFromDto(baseRequest, existing);
        assertThat(existing.getName()).isEqualTo("NewCo");
        assertThat(existing.getPhone()).isEqualTo("0909999999");
    }

    @Test
    void updateSupplier_shouldSkipUniqueChecksWhenUnchanged() {
        Supplier existing = buildSupplier(8L, "ACME", "0909123456");
        SupplierDTO response = buildResponse(8L, "ACME", "0909123456");

        when(supplierValidator.requireExistingSupplier(8L)).thenReturn(existing);
        when(supplierValidator.normalizeName("   ACME   ")).thenReturn("ACME");
        when(supplierValidator.normalizePhone(" 0909 123 456 ")).thenReturn("0909123456");
        when(supplierRepository.save(existing)).thenReturn(existing);
        when(supplierMapper.toDto(existing)).thenReturn(response);

        SupplierDTO result = supplierService.updateSupplier(8L, baseRequest);

        assertThat(result).isEqualTo(response);
        verify(supplierValidator, never()).ensureNameUnique(any(), eq(8L));
        verify(supplierValidator, never()).ensurePhoneUnique(any(), eq(8L));
    }

    @Test
    void deleteSupplier_shouldUseValidatorAndRepository() {
        Supplier existing = buildSupplier(9L, "ACME", "0909123456");
        when(supplierValidator.requireExistingSupplier(9L)).thenReturn(existing);

        supplierService.deleteSupplier(9L);

        verify(supplierValidator).requireExistingSupplier(9L);
        verify(supplierRepository).delete(existing);
    }

    private SupplierDTO buildRequest(String name, String phone) {
        SupplierDTO dto = new SupplierDTO();
        dto.setName(name);
        dto.setPhone(phone);
        dto.setContactPerson("John Doe");
        dto.setEmail("john@example.com");
        dto.setAddress("123 Street");
        return dto;
    }

    private Supplier buildSupplier(Long id, String name, String phone) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setName(name);
        supplier.setPhone(phone);
        supplier.setContactPerson("John Doe");
        supplier.setEmail("john@example.com");
        supplier.setAddress("123 Street");
        return supplier;
    }

    private SupplierDTO buildResponse(Long id, String name, String phone) {
        SupplierDTO dto = new SupplierDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setPhone(phone);
        dto.setContactPerson("John Doe");
        dto.setEmail("john@example.com");
        dto.setAddress("123 Street");
        return dto;
    }
}
