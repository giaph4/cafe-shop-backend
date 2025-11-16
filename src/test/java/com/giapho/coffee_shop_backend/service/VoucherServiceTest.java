package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.domain.repository.VoucherRepository;
import com.giapho.coffee_shop_backend.dto.VoucherRequestDTO;
import com.giapho.coffee_shop_backend.dto.VoucherResponseDTO;
import com.giapho.coffee_shop_backend.dto.VoucherSummaryDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

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
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @InjectMocks
    private VoucherService voucherService;

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
        when(voucherRepository.existsByCodeIgnoreCase("SAVE20")).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> {
            Voucher voucher = invocation.getArgument(0);
            voucher.setId(10L);
            return voucher;
        });

        VoucherResponseDTO response = voucherService.createVoucher(baseRequest);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCode()).isEqualTo("SAVE20");
        assertThat(response.getTimesUsed()).isZero();
        verify(voucherRepository).existsByCodeIgnoreCase("SAVE20");
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    void createVoucher_shouldThrowWhenCodeExists() {
        when(voucherRepository.existsByCodeIgnoreCase("SAVE20")).thenReturn(true);

        assertThatThrownBy(() -> voucherService.createVoucher(baseRequest))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Voucher code đã tồn tại");
    }

    @Test
    void updateVoucher_shouldValidateUsageLimit() {
        Voucher existing = buildVoucherEntity();
        existing.setTimesUsed(5);

        when(voucherRepository.findById(1L)).thenReturn(Optional.of(existing));

        VoucherRequestDTO request = baseRequest.toBuilder()
                .usageLimit(4)
                .build();

        assertThatThrownBy(() -> voucherService.updateVoucher(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("usageLimit");
    }

    @Test
    void updateVoucher_shouldPersistChanges() {
        Voucher existing = buildVoucherEntity();
        existing.setId(1L);

        when(voucherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoucherRequestDTO request = baseRequest.toBuilder()
                .description("Cập nhật mô tả")
                .usageLimit(200)
                .build();

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
                .isInstanceOf(IllegalStateException.class)
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
        when(voucherRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(buildVoucherEntity())));

        Page<VoucherResponseDTO> result = voucherService.searchVouchers("save", Voucher.VoucherType.FIXED_AMOUNT, true,
                baseRequest.getValidFrom(), baseRequest.getValidTo(), pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(voucherRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchVouchers_shouldBuildSpecificationWithAllFilters() {
        PageRequest pageable = PageRequest.of(0, 5);
        ArgumentCaptor<Specification<Voucher>> specCaptor = ArgumentCaptor.forClass(Specification.class);

        when(voucherRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(buildVoucherEntity())));

        LocalDateTime validFrom = baseRequest.getValidFrom();
        LocalDateTime validTo = baseRequest.getValidTo();

        voucherService.searchVouchers(" Save ", Voucher.VoucherType.FIXED_AMOUNT, Boolean.TRUE, validFrom, validTo, pageable);

        verify(voucherRepository).findAll(specCaptor.capture(), eq(pageable));

        Specification<Voucher> specification = specCaptor.getValue();
        assertThat(specification).isNotNull();

        @SuppressWarnings("unchecked")
        Root<Voucher> root = (Root<Voucher>) mock(Root.class);
        Path<?> codePath = mock(Path.class);
        Path<?> typePath = mock(Path.class);
        Path<?> activePath = mock(Path.class);
        Path<?> validFromPath = mock(Path.class);
        Path<?> validToPath = mock(Path.class);

        when(root.get("code")).thenReturn((Path) codePath);
        when(root.get("type")).thenReturn((Path) typePath);
        when(root.get("active")).thenReturn((Path) activePath);
        when(root.get("validFrom")).thenReturn((Path) validFromPath);
        when(root.get("validTo")).thenReturn((Path) validToPath);

        CriteriaQuery<Voucher> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Expression<String> loweredCode = mock(Expression.class);
        Predicate codePredicate = mock(Predicate.class);
        Predicate typePredicate = mock(Predicate.class);
        Predicate activePredicate = mock(Predicate.class);
        Predicate fromPredicate = mock(Predicate.class);
        Predicate toPredicate = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(cb.lower((Expression<String>) codePath)).thenReturn(loweredCode);
        when(cb.like(loweredCode, "%save%"))
                .thenReturn(codePredicate);
        when(cb.equal((Expression<?>) typePath, Voucher.VoucherType.FIXED_AMOUNT))
                .thenReturn(typePredicate);
        when(cb.equal((Expression<?>) activePath, Boolean.TRUE))
                .thenReturn(activePredicate);
        when(cb.greaterThanOrEqualTo((Expression) validFromPath, validFrom))
                .thenReturn(fromPredicate);
        when(cb.lessThanOrEqualTo((Expression) validToPath, validTo))
                .thenReturn(toPredicate);
        lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(combined);
        lenient().when(cb.and(any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(combined);
        lenient().when(cb.and(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class))).thenReturn(combined);
        lenient().when(cb.and(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class)))
                .thenReturn(combined);

        Predicate actual = specification.toPredicate(root, query, cb);

        assertThat(actual).isNotNull();

        verify(cb).like(loweredCode, "%save%");
        verify(cb).equal((Expression<?>) typePath, Voucher.VoucherType.FIXED_AMOUNT);
        verify(cb).equal((Expression<?>) activePath, Boolean.TRUE);
        verify(cb).greaterThanOrEqualTo((Expression) validFromPath, validFrom);
        verify(cb).lessThanOrEqualTo((Expression) validToPath, validTo);
        verify(cb, atLeastOnce()).and(any(Predicate.class), any(Predicate.class));
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
        when(voucherRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.empty());

        voucherService.incrementUsageCount("SAVE20");

        verify(voucherRepository, never()).save(any());
    }

    @Test
    void incrementUsageCount_shouldIncreaseTimesUsed() {
        Voucher voucher = buildVoucherEntity();
        voucher.setTimesUsed(2);
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
                .isInstanceOf(EntityNotFoundException.class)
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
