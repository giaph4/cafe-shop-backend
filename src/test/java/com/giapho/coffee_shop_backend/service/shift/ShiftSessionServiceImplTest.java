package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.Role;
import com.giapho.coffee_shop_backend.domain.entity.ShiftSession;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.entity.WorkShift;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftSessionStatus;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftSessionRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.domain.repository.WorkShiftRepository;
import com.giapho.coffee_shop_backend.dto.shift.ShiftReportPaymentBreakdownDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftReportProductDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftReportResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftSessionResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftSessionStartRequestDTO;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAccessDeniedException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftSessionAlreadyActiveException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftSessionInvalidStateException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftSessionLimitReachedException;
import com.giapho.coffee_shop_backend.exception.user.UserNotAuthenticatedException;
import com.giapho.coffee_shop_backend.mapper.ShiftSessionMapper;
import com.giapho.coffee_shop_backend.service.shift.impl.ShiftSessionServiceImpl;
import com.giapho.coffee_shop_backend.shift.messaging.ShiftRealtimePublisher;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ShiftSessionServiceImplTest {

    @Mock
    private ShiftSessionRepository shiftSessionRepository;
    @Mock
    private WorkShiftRepository workShiftRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ShiftSessionMapper shiftSessionMapper;
    @Mock
    private ShiftReportService shiftReportService;
    @Mock
    private ShiftRealtimePublisher shiftRealtimePublisher;

    @InjectMocks
    private ShiftSessionServiceImpl shiftSessionService;

    private MockedStatic<SecurityUtil> securityUtilMock;
    private User currentUser;
    private WorkShift workShift;

    @BeforeEach
    void setUp() {
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentUsername).thenReturn(Optional.of("staff01"));
        currentUser = userWithRoles(5L, "staff01", "ROLE_STAFF");
        workShift = WorkShift.builder()
                .id(3L)
                .name("Ca sáng")
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(4))
                .maxEmployees(2)
                .isActive(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    @Test
    void startSession_ShouldCreateNewActiveSession() {
        stubCurrentUser(currentUser);
        ShiftSessionStartRequestDTO request = new ShiftSessionStartRequestDTO(workShift.getId(), false);
        when(workShiftRepository.findById(workShift.getId())).thenReturn(Optional.of(workShift));
        when(shiftSessionRepository.findFirstByUser_IdAndStatus(currentUser.getId(), ShiftSessionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(shiftSessionRepository.countByWorkShift_IdAndStatus(workShift.getId(), ShiftSessionStatus.ACTIVE))
                .thenReturn(1L);

        ShiftSession session = ShiftSession.builder()
                .id(100L)
                .workShift(workShift)
                .user(currentUser)
                .startAt(LocalDateTime.now())
                .status(ShiftSessionStatus.ACTIVE)
                .build();
        when(shiftSessionRepository.save(any(ShiftSession.class))).thenReturn(session);
        ShiftSessionResponseDTO expectedResponse = new ShiftSessionResponseDTO(
                session.getId(), workShift.getId(), currentUser.getId(), currentUser.getUsername(),
                currentUser.getFullName(), session.getStartAt(), null, ShiftSessionStatus.ACTIVE,
                false, null, null, session.getStartAt(), session.getStartAt()
        );
        when(shiftSessionMapper.toResponse(session)).thenReturn(expectedResponse);

        ShiftSessionResponseDTO response = shiftSessionService.startSession(request);

        assertThat(response.id()).isEqualTo(100L);
        verify(shiftSessionRepository).save(any(ShiftSession.class));
        verify(shiftRealtimePublisher).publishSessionStarted(expectedResponse);
    }

    @Test
    void startSession_ShouldThrow_WhenShiftInactive() {
        workShift.setActive(false);
        stubCurrentUser(currentUser);
        when(workShiftRepository.findById(workShift.getId())).thenReturn(Optional.of(workShift));

        assertThrows(ShiftSessionInvalidStateException.class,
                () -> shiftSessionService.startSession(new ShiftSessionStartRequestDTO(workShift.getId(), false)));
    }

    @Test
    void startSession_ShouldThrow_WhenUserAlreadyActive() {
        stubCurrentUser(currentUser);
        when(workShiftRepository.findById(workShift.getId())).thenReturn(Optional.of(workShift));
        when(shiftSessionRepository.findFirstByUser_IdAndStatus(currentUser.getId(), ShiftSessionStatus.ACTIVE))
                .thenReturn(Optional.of(new ShiftSession()));

        assertThrows(ShiftSessionAlreadyActiveException.class,
                () -> shiftSessionService.startSession(new ShiftSessionStartRequestDTO(workShift.getId(), false)));
    }

    @Test
    void startSession_ShouldAllowAdminOverrideWhenFull() {
        currentUser = userWithRoles(7L, "admin01", "ROLE_ADMIN");
        stubCurrentUser(currentUser);
        when(workShiftRepository.findById(workShift.getId())).thenReturn(Optional.of(workShift));
        when(shiftSessionRepository.findFirstByUser_IdAndStatus(currentUser.getId(), ShiftSessionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        ShiftSessionStartRequestDTO request = new ShiftSessionStartRequestDTO(workShift.getId(), true);
        ShiftSession session = ShiftSession.builder()
                .id(200L)
                .workShift(workShift)
                .user(currentUser)
                .startAt(LocalDateTime.now())
                .status(ShiftSessionStatus.ACTIVE)
                .adminOverride(true)
                .build();
        when(shiftSessionRepository.save(any(ShiftSession.class))).thenAnswer(invocation -> {
            ShiftSession saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });
        ShiftSessionResponseDTO dto = new ShiftSessionResponseDTO(
                session.getId(), workShift.getId(), currentUser.getId(), currentUser.getUsername(),
                null, session.getStartAt(), null, ShiftSessionStatus.ACTIVE,
                true, null, null, session.getStartAt(), session.getStartAt()
        );
        when(shiftSessionMapper.toResponse(any(ShiftSession.class))).thenReturn(dto);

        shiftSessionService.startSession(request);
        verify(shiftSessionRepository).save(any(ShiftSession.class));
        verify(shiftRealtimePublisher).publishSessionStarted(dto);
    }

    @Test
    void startSession_ShouldThrow_WhenShiftFullAndOverrideFalse() {
        stubCurrentUser(currentUser);
        when(workShiftRepository.findById(workShift.getId())).thenReturn(Optional.of(workShift));
        when(shiftSessionRepository.findFirstByUser_IdAndStatus(currentUser.getId(), ShiftSessionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(shiftSessionRepository.countByWorkShift_IdAndStatus(workShift.getId(), ShiftSessionStatus.ACTIVE))
                .thenReturn((long) workShift.getMaxEmployees());

        assertThrows(ShiftSessionLimitReachedException.class,
                () -> shiftSessionService.startSession(new ShiftSessionStartRequestDTO(workShift.getId(), false)));
    }

    @Test
    void endCurrentSession_ShouldCloseAndTransferOrders() {
        stubCurrentUser(currentUser);
        ShiftSession activeSession = ShiftSession.builder()
                .id(300L)
                .user(currentUser)
                .status(ShiftSessionStatus.ACTIVE)
                .startAt(LocalDateTime.now().minusHours(2))
                .build();
        when(shiftSessionRepository.findFirstByUser_IdAndStatus(currentUser.getId(), ShiftSessionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSession));
        Order unpaid = Order.builder().id(10L).status(OrderStatus.PENDING).build();
        Order paid = Order.builder().id(11L).status(OrderStatus.PAID).build();
        when(shiftSessionRepository.findWithUserAndWorkShiftById(activeSession.getId())).thenReturn(Optional.of(activeSession));
        when(orderRepository.findByShiftSessionIdWithDetails(activeSession.getId())).thenReturn(List.of(unpaid, paid));
        when(shiftSessionRepository.save(activeSession)).thenReturn(activeSession);
        ShiftReportResponseDTO report = sampleReportDto();
        when(shiftReportService.generateReport(activeSession, List.of(unpaid, paid))).thenReturn(report);
        ShiftSessionResponseDTO sessionDto = new ShiftSessionResponseDTO(
                activeSession.getId(), null, null, null, null,
                activeSession.getStartAt(), LocalDateTime.now(), ShiftSessionStatus.CLOSED,
                false, null, null, activeSession.getStartAt(), LocalDateTime.now()
        );
        when(shiftSessionMapper.toResponse(activeSession)).thenReturn(sessionDto);

        ShiftSessionResponseDTO result = shiftSessionService.endCurrentSession();

        assertThat(result.status()).isEqualTo(ShiftSessionStatus.CLOSED);
        assertThat(unpaid.isTransferred()).isTrue();
        assertThat(unpaid.getShiftSession()).isNull();
        verify(orderRepository).save(unpaid);
        verify(shiftReportService).generateReport(activeSession, List.of(unpaid, paid));
        verify(shiftRealtimePublisher).publishSessionEnded(sessionDto, report);
    }

    @Test
    void forceEndSession_ShouldRequireManagerRole() {
        User manager = userWithRoles(8L, "manager01", "ROLE_MANAGER");
        stubCurrentUser(manager);

        ShiftSession session = ShiftSession.builder()
                .id(400L)
                .status(ShiftSessionStatus.ACTIVE)
                .startAt(LocalDateTime.now().minusHours(1))
                .build();

        when(shiftSessionRepository.findWithUserAndWorkShiftById(400L)).thenReturn(Optional.of(session));
        when(orderRepository.findByShiftSessionIdWithDetails(400L)).thenReturn(List.of());
        ShiftReportResponseDTO report = sampleReportDto();
        when(shiftReportService.generateReport(session, List.of())).thenReturn(report);
        when(shiftSessionRepository.save(session)).thenReturn(session);
        ShiftSessionResponseDTO dto = new ShiftSessionResponseDTO(
                session.getId(), null, null, null, null,
                session.getStartAt(), session.getEndAt(), ShiftSessionStatus.FORCED,
                session.isAdminOverride(), "reason", manager.getId(), session.getStartAt(), session.getEndAt()
        );
        when(shiftSessionMapper.toResponse(session)).thenReturn(dto);

        ShiftSessionResponseDTO response = shiftSessionService.forceEndSession(400L, "reason");
        assertThat(response.status()).isEqualTo(ShiftSessionStatus.FORCED);
        verify(shiftReportService).generateReport(session, List.of());
        verify(shiftRealtimePublisher).publishSessionForced(dto, report);
    }

    @Test
    void forceEndSession_ShouldThrow_WhenActorNotManagerOrAdmin() {
        stubCurrentUser(currentUser);

        assertThrows(ShiftAccessDeniedException.class,
                () -> shiftSessionService.forceEndSession(500L, "reason"));
    }

    @Test
    void forceEndSession_ShouldThrow_WhenSessionNotActive() {
        User manager = userWithRoles(8L, "manager01", "ROLE_MANAGER");
        stubCurrentUser(manager);
        ShiftSession session = ShiftSession.builder()
                .id(600L)
                .status(ShiftSessionStatus.CLOSED)
                .build();
        when(shiftSessionRepository.findWithUserAndWorkShiftById(600L)).thenReturn(Optional.of(session));

        assertThrows(ShiftSessionInvalidStateException.class,
                () -> shiftSessionService.forceEndSession(600L, "reason"));
    }

    @Test
    void getCurrentActiveSession_ShouldReturnDto() {
        stubCurrentUser(currentUser);
        ShiftSession session = ShiftSession.builder()
                .id(700L)
                .user(currentUser)
                .status(ShiftSessionStatus.ACTIVE)
                .startAt(LocalDateTime.now())
                .build();
        when(shiftSessionRepository.findFirstByUser_IdAndStatus(currentUser.getId(), ShiftSessionStatus.ACTIVE))
                .thenReturn(Optional.of(session));
        when(shiftSessionMapper.toResponse(session)).thenReturn(new ShiftSessionResponseDTO(
                session.getId(), null, currentUser.getId(), currentUser.getUsername(), null,
                session.getStartAt(), null, ShiftSessionStatus.ACTIVE,
                false, null, null, session.getStartAt(), session.getStartAt()
        ));

        ShiftSessionResponseDTO dto = shiftSessionService.getCurrentActiveSession();
        assertThat(dto.status()).isEqualTo(ShiftSessionStatus.ACTIVE);
    }

    @Test
    void listActiveSessionsByShift_ShouldReturnMappedDtos() {
        ShiftSession session = ShiftSession.builder().id(800L).status(ShiftSessionStatus.ACTIVE).build();
        when(shiftSessionRepository.findByWorkShift_IdAndStatus(1L, ShiftSessionStatus.ACTIVE))
                .thenReturn(List.of(session));
        when(shiftSessionMapper.toResponse(session)).thenReturn(new ShiftSessionResponseDTO(
                800L, 1L, null, null, null, LocalDateTime.now(), null,
                ShiftSessionStatus.ACTIVE, false, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        ));

        List<ShiftSessionResponseDTO> dtos = shiftSessionService.listActiveSessionsByShift(1L);
        assertThat(dtos).hasSize(1);
    }

    @Test
    void startSession_ShouldValidateAuthentication() {
        securityUtilMock.when(SecurityUtil::getCurrentUsername).thenReturn(Optional.empty());

        assertThrows(UserNotAuthenticatedException.class,
                () -> shiftSessionService.startSession(new ShiftSessionStartRequestDTO(1L, false)));
    }

    private User userWithRoles(Long userId, String username, String... roles) {
        java.util.Set<Role> roleEntities = java.util.Arrays.stream(roles)
                .map(role -> Role.builder().name(role).build())
                .collect(java.util.stream.Collectors.toSet());

        User user = User.builder()
                .id(userId)
                .username(username)
                .roles(roleEntities)
                .build();
        return user;
    }

    private void stubCurrentUser(User user) {
        when(userRepository.findWithRolesByUsername("staff01")).thenReturn(Optional.of(user));
    }

    private ShiftReportResponseDTO sampleReportDto() {
        return new ShiftReportResponseDTO(
                1L,
                300L,
                10L,
                5L,
                "staff01",
                ShiftSessionStatus.CLOSED,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now(),
                2,
                java.math.BigDecimal.TEN,
                java.math.BigDecimal.ZERO,
                1,
                List.of(new ShiftReportPaymentBreakdownDTO("CASH", 1, java.math.BigDecimal.TEN)),
                List.of(new ShiftReportProductDTO(100L, "Bạc xỉu", 2, java.math.BigDecimal.TEN)),
                LocalDateTime.now()
        );
    }
}
