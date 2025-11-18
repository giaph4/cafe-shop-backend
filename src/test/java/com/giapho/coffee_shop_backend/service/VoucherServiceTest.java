package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.domain.repository.VoucherRepository;
import com.giapho.coffee_shop_backend.dto.VoucherRequestDTO;
import com.giapho.coffee_shop_backend.dto.VoucherResponseDTO;
import com.giapho.coffee_shop_backend.dto.VoucherSummaryDTO;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherConflictException;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherInvalidException;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherNotFoundException;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherValidationException;
import com.giapho.coffee_shop_backend.service.impl.VoucherServiceImpl;
import com.giapho.coffee_shop_backend.service.voucher.helper.VoucherDiscountCalculator;
import com.giapho.coffee_shop_backend.service.voucher.helper.VoucherMapper;
import com.giapho.coffee_shop_backend.service.voucher.helper.VoucherSearchSpecificationBuilder;
import com.giapho.coffee_shop_backend.service.voucher.helper.VoucherValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;
    @Mock
    private VoucherValidator voucherValidator;
    @Mock
    private VoucherMapper voucherMapper;
    @Mock
    private VoucherDiscountCalculator discountCalculator;
    @Mock
    private VoucherSearchSpecificationBuilder specificationBuilder;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    private VoucherRequestDTO baseRequest;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        baseRequest = VoucherRequestDTO.builder()
                .code("save20")
                .description("Giảm 20K cho hóa đơn trên 100K")
                .type(Voucher.VoucherType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("20000"))
                .minimumOrderAmount(new BigDecimal("100000"))
                .maximumDiscountAmount(null)
                .validFrom(now)
                .validTo(now.plusDays(7))
                .usageLimit(50)
                .active(true)
                .build();
    }

    @Test
    void createVoucher_shouldNormalizeCodeAndPersist() {
        when(voucherValidator.normalizeCode("save20")).thenReturn("SAVE20");
        when(voucherMapper.toEntity(baseRequest)).thenReturn(new Voucher());
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher voucher = invocation.getArgument(0);
            voucher.setId(10L);
            return voucher;
        });
        when(voucherMapper.toResponse(any(Voucher.class))).thenReturn(VoucherResponseDTO.builder().id(10L).code("SAVE20").timesUsed(0).build());

        VoucherResponseDTO response = voucherService.createVoucher(baseRequest);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCode()).isEqualTo("SAVE20");
        verify(voucherValidator).ensureCodeUnique(eq("SAVE20"), any());
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    void createVoucher_shouldThrowWhenCodeExists() {
        when(voucherValidator.normalizeCode("save20")).thenReturn("SAVE20");
        doThrow(new VoucherConflictException("Voucher code đã tồn tại: SAVE20"))
                .when(voucherValidator).ensureCodeUnique(eq("SAVE20"), any());

        assertThatThrownBy(() -> voucherService.createVoucher(baseRequest))
                .isInstanceOf(VoucherConflictException.class)
                .hasMessageContaining("Voucher code đã tồn tại");
    }

    @Test
    void updateVoucher_shouldValidateUsageLimit() {
        Voucher existing = buildVoucherEntity();
        existing.setTimesUsed(5);

        when(voucherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(voucherValidator.normalizeCode("save20")).thenReturn("SAVE20");
        VoucherRequestDTO request = baseRequest.toBuilder().usageLimit(4).build();
        doThrow(new VoucherValidationException("usageLimit không thể nhỏ hơn số lượt đã sử dụng"))
                .when(voucherValidator).ensureUsageLimitNotLessThanTimesUsed(existing, 4);

        assertThatThrownBy(() -> voucherService.updateVoucher(1L, request))
                .isInstanceOf(VoucherValidationException.class)
                .hasMessageContaining("usageLimit");
    }

    @Test
    void updateVoucher_shouldPersistChanges() {
        Voucher existing = buildVoucherEntity();
        existing.setId(1L);

        when(voucherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(voucherValidator.normalizeCode("save20")).thenReturn("SAVE20");
        when(voucherMapper.toResponse(any(Voucher.class))).thenReturn(VoucherResponseDTO.builder()
                .description("Cập nhật mô tả")
                .usageLimit(200)
                .build());
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoucherRequestDTO request = baseRequest.toBuilder().description("Cập nhật mô tả").usageLimit(200).build();

        VoucherResponseDTO response = voucherService.updateVoucher(1L, request);

        assertThat(response.getDescription()).isEqualTo("Cập nhật mô tả");
        assertThat(response.getUsageLimit()).isEqualTo(200);
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    void toggleVoucherActive_shouldFlipState() {
        Voucher existing = buildVoucherEntity();
        existing.setActive(true);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(voucherMapper.toResponse(any(Voucher.class))).thenReturn(VoucherResponseDTO.builder().active(false).build());
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoucherResponseDTO response = voucherService.toggleVoucherActive(1L);

        assertThat(response.getActive()).isFalse();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    void deleteVoucher_shouldRejectWhenUsed() {
        Voucher existing = buildVoucherEntity();
        existing.setTimesUsed(1);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> voucherService.deleteVoucher(1L))
                .isInstanceOf(VoucherInvalidException.class)
                .hasMessageContaining("Không thể xóa voucher");
    }

    @Test
    void deleteVoucher_shouldRemoveWhenUnused() {
        Voucher existing = buildVoucherEntity();
        existing.setTimesUsed(0);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(existing));

        voucherService.deleteVoucher(1L);

        verify(voucherRepository).delete(existing);
    }

    @Test
    void searchVouchers_shouldDelegateToRepository() {
        PageRequest pageable = PageRequest.of(0, 10);
        Specification<Voucher> specification = mock(Specification.class);
        when(specificationBuilder.build(eq("save"), eq(Voucher.VoucherType.FIXED_AMOUNT), eq(Boolean.TRUE), any(), any()))
                .thenReturn(specification);
        when(voucherRepository.findAll(specification, pageable))
                .thenReturn(new PageImpl<>(List.of(buildVoucherEntity())));

        Page<VoucherResponseDTO> result = voucherService.searchVouchers("save", Voucher.VoucherType.FIXED_AMOUNT, true,
                baseRequest.getValidFrom(), baseRequest.getValidTo(), pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(voucherRepository).findAll(specification, pageable);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchVouchers_shouldBuildSpecificationWithAllFilters() {
        PageRequest pageable = PageRequest.of(0, 5);
        LocalDateTime validFrom = baseRequest.getValidFrom();
        LocalDateTime validTo = baseRequest.getValidTo();

        Specification<Voucher> specification = mock(Specification.class);
        when(specificationBuilder.build(" Save ", Voucher.VoucherType.FIXED_AMOUNT, Boolean.TRUE, validFrom, validTo))
                .thenReturn(specification);
        when(voucherRepository.findAll(specification, pageable))
                .thenReturn(new PageImpl<>(List.of(buildVoucherEntity())));

        voucherService.searchVouchers(" Save ", Voucher.VoucherType.FIXED_AMOUNT, Boolean.TRUE, validFrom, validTo, pageable);

        verify(specificationBuilder).build(" Save ", Voucher.VoucherType.FIXED_AMOUNT, Boolean.TRUE, validFrom, validTo);
        verify(voucherRepository).findAll(specification, pageable);
    }

    @Test
    void getVoucherSummary_shouldAggregateCounts() {
        when(voucherRepository.countByActiveTrue()).thenReturn(5L);
        when(voucherRepository.countByActiveFalse()).thenReturn(2L);
        when(voucherRepository.countByValidToBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);
        when(voucherRepository.sumTimesUsed()).thenReturn(40L);

        VoucherSummaryDTO summary = voucherService.getVoucherSummary();

        assertThat(summary.getActiveCount()).isEqualTo(5L);
        assertThat(summary.getRedeemedCount()).isEqualTo(40L);
    }

    @Test
    void incrementUsageCount_shouldIgnoreMissingVoucher() {
        when(voucherValidator.hasText("SAVE20")).thenReturn(true);
        when(voucherValidator.normalizeCode("SAVE20")).thenReturn("SAVE20");
        when(voucherRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.empty());

        voucherService.incrementUsageCount("SAVE20");

        verify(voucherRepository, never()).save(any());
    }

    @Test
    void incrementUsageCount_shouldIncreaseTimesUsed() {
        Voucher voucher = buildVoucherEntity();
        voucher.setTimesUsed(2);
        when(voucherValidator.hasText(" save20 ")).thenReturn(true);
        when(voucherValidator.normalizeCode(" save20 ")).thenReturn("SAVE20");
        when(voucherRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(voucher));

        voucherService.incrementUsageCount(" save20 ");

        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(captor.capture());
        assertThat(captor.getValue().getTimesUsed()).isEqualTo(3);
    }

    @Test
    void getVoucherById_shouldThrowWhenMissing() {
        when(voucherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voucherService.getVoucherById(99L))
                .isInstanceOf(VoucherNotFoundException.class)
                .hasMessageContaining("Voucher không tồn tại");
    }

    private Voucher buildVoucherEntity() {
        Voucher voucher = new Voucher();
        voucher.setCode("SAVE20");
        voucher.setDescription("Giảm giá");
        voucher.setType(Voucher.VoucherType.FIXED_AMOUNT);
        voucher.setDiscountValue(new BigDecimal("20000"));
        voucher.setMinimumOrderAmount(new BigDecimal("100000"));
        voucher.setMaximumDiscountAmount(null);
        voucher.setValidFrom(LocalDateTime.now().minusDays(1));
        voucher.setValidTo(LocalDateTime.now().plusDays(5));
        voucher.setUsageLimit(100);
        voucher.setTimesUsed(0);
        voucher.setActive(true);
        voucher.setCreatedAt(LocalDateTime.now());
        voucher.setUpdatedAt(LocalDateTime.now());
        return voucher;
    }
}
