package com.giapho.coffee_shop_backend.service.shift.impl;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.ShiftSession;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.entity.WorkShift;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftSessionStatus;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftSessionRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.domain.repository.WorkShiftRepository;
import com.giapho.coffee_shop_backend.dto.shift.ShiftReportResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftSessionResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftSessionStartRequestDTO;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAccessDeniedException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftSessionAlreadyActiveException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftSessionInvalidStateException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftSessionLimitReachedException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftSessionNotFoundException;
import com.giapho.coffee_shop_backend.exception.shift.WorkShiftNotFoundException;
import com.giapho.coffee_shop_backend.exception.user.UserNotAuthenticatedException;
import com.giapho.coffee_shop_backend.exception.user.UserNotFoundException;
import com.giapho.coffee_shop_backend.mapper.ShiftSessionMapper;
import com.giapho.coffee_shop_backend.service.shift.ShiftReportService;
import com.giapho.coffee_shop_backend.service.shift.ShiftSessionService;
import com.giapho.coffee_shop_backend.shift.messaging.ShiftRealtimePublisher;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShiftSessionServiceImpl implements ShiftSessionService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_MANAGER = "ROLE_MANAGER";

    private final ShiftSessionRepository shiftSessionRepository;
    private final WorkShiftRepository workShiftRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ShiftSessionMapper shiftSessionMapper;
    private final ShiftReportService shiftReportService;
    private final ShiftRealtimePublisher shiftRealtimePublisher;

    @Override
    public ShiftSessionResponseDTO startSession(ShiftSessionStartRequestDTO request) {
        User currentUser = resolveCurrentUser();
        WorkShift workShift = workShiftRepository.findById(request.workShiftId())
                .orElseThrow(() -> new WorkShiftNotFoundException(request.workShiftId()));

        validateShiftForStart(workShift);
        ensureNoActiveSession(currentUser.getId());
        ensureCapacity(workShift, currentUser, Boolean.TRUE.equals(request.adminOverride()));

        ShiftSession session = ShiftSession.builder()
                .workShift(workShift)
                .user(currentUser)
                .startAt(LocalDateTime.now())
                .status(ShiftSessionStatus.ACTIVE)
                .adminOverride(Boolean.TRUE.equals(request.adminOverride()))
                .build();

        ShiftSession saved = shiftSessionRepository.save(session);
        ShiftSessionResponseDTO response = shiftSessionMapper.toResponse(saved);
        shiftRealtimePublisher.publishSessionStarted(response);
        return response;
    }

    @Override
    public ShiftSessionResponseDTO endCurrentSession() {
        ShiftSession session = findCurrentSessionForCurrentUser();
        session = loadSessionWithRelations(session.getId()).orElse(session);

        closeSession(session, ShiftSessionStatus.CLOSED, null, null);

        List<Order> orders = orderRepository.findByShiftSessionIdWithDetails(session.getId());
        handleUnpaidOrders(orders);

        ShiftReportResponseDTO report = shiftReportService.generateReport(session, orders);

        ShiftSession saved = shiftSessionRepository.save(session);
        ShiftSessionResponseDTO response = shiftSessionMapper.toResponse(saved);
        shiftRealtimePublisher.publishSessionEnded(response, report);
        return response;
    }

    @Override
    public ShiftSessionResponseDTO forceEndSession(Long sessionId, String reason) {
        User actor = resolveCurrentUser();
        ensureManagerOrAdmin(actor);

        ShiftSession session = loadSessionWithRelations(sessionId)
                .orElseThrow(() -> new ShiftSessionNotFoundException(sessionId));
        if (session.getStatus() != ShiftSessionStatus.ACTIVE) {
            throw new ShiftSessionInvalidStateException("Cannot force end a session that is not active");
        }

        closeSession(session, ShiftSessionStatus.FORCED, reason, actor);
        List<Order> orders = orderRepository.findByShiftSessionIdWithDetails(session.getId());
        handleUnpaidOrders(orders);

        ShiftReportResponseDTO report = shiftReportService.generateReport(session, orders);

        ShiftSession saved = shiftSessionRepository.save(session);
        ShiftSessionResponseDTO response = shiftSessionMapper.toResponse(saved);
        shiftRealtimePublisher.publishSessionForced(response, report);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftSessionResponseDTO getCurrentActiveSession() {
        ShiftSession session = findCurrentSessionForCurrentUser();
        return shiftSessionMapper.toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftSessionResponseDTO> listActiveSessionsByShift(Long workShiftId) {
        return shiftSessionRepository.findByWorkShift_IdAndStatus(workShiftId, ShiftSessionStatus.ACTIVE)
                .stream()
                .map(shiftSessionMapper::toResponse)
                .toList();
    }

    private void ensureManagerOrAdmin(User actor) {
        boolean hasAuthority = actor.getAuthorities().stream()
                .anyMatch(ga -> ROLE_ADMIN.equals(ga.getAuthority()) || ROLE_MANAGER.equals(ga.getAuthority()));
        if (!hasAuthority) {
            throw new ShiftAccessDeniedException("Chỉ ADMIN hoặc MANAGER mới được phép force end ca");
        }
    }

    private ShiftSession findCurrentSessionForCurrentUser() {
        User currentUser = resolveCurrentUser();
        return shiftSessionRepository.findFirstByUser_IdAndStatus(currentUser.getId(), ShiftSessionStatus.ACTIVE)
                .orElseThrow(ShiftSessionNotFoundException::new);
    }

    private void ensureCapacity(WorkShift workShift, User user, boolean adminOverride) {
        if (adminOverride && isAdminOrManager(user)) {
            return;
        }
        long activeCount = shiftSessionRepository.countByWorkShift_IdAndStatus(workShift.getId(), ShiftSessionStatus.ACTIVE);
        if (activeCount >= workShift.getMaxEmployees()) {
            throw new ShiftSessionLimitReachedException(workShift.getId());
        }
    }

    private boolean isAdminOrManager(User user) {
        return user.getAuthorities().stream()
                .anyMatch(ga -> ROLE_ADMIN.equals(ga.getAuthority()) || ROLE_MANAGER.equals(ga.getAuthority()));
    }

    private void ensureNoActiveSession(Long userId) {
        shiftSessionRepository.findFirstByUser_IdAndStatus(userId, ShiftSessionStatus.ACTIVE)
                .ifPresent(session -> {
                    throw new ShiftSessionAlreadyActiveException(userId);
                });
    }

    private void validateShiftForStart(WorkShift workShift) {
        if (!workShift.isActive()) {
            throw new ShiftSessionInvalidStateException("Ca làm việc đã bị vô hiệu hoá");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(workShift.getStartAt()) || now.isAfter(workShift.getEndAt())) {
            throw new ShiftSessionInvalidStateException("Chưa đến thời gian vào ca hoặc ca đã kết thúc");
        }
    }

    private void closeSession(ShiftSession session, ShiftSessionStatus status, String reason, User actor) {
        if (session.getStatus() != ShiftSessionStatus.ACTIVE) {
            throw new ShiftSessionInvalidStateException("Phiên làm việc không ở trạng thái ACTIVE");
        }
        session.setStatus(status);
        session.setEndAt(LocalDateTime.now());
        session.setForceReason(reason);
        session.setForceBy(actor);
    }

    private void handleUnpaidOrders(List<Order> orders) {
        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.PAID) {
                order.setShiftSession(null);
                order.setTransferred(true);
                orderRepository.save(order);
            }
        }
    }

    private Optional<ShiftSession> loadSessionWithRelations(Long sessionId) {
        return shiftSessionRepository.findWithUserAndWorkShiftById(sessionId);
    }

    private User resolveCurrentUser() {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(UserNotAuthenticatedException::new);
        return userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }
}
