package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.dto.shift.ShiftSessionResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftSessionStartRequestDTO;

import java.util.List;

public interface ShiftSessionService {

    ShiftSessionResponseDTO startSession(ShiftSessionStartRequestDTO request);

    ShiftSessionResponseDTO endCurrentSession();

    ShiftSessionResponseDTO forceEndSession(Long sessionId, String reason);

    ShiftSessionResponseDTO getCurrentActiveSession();

    List<ShiftSessionResponseDTO> listActiveSessionsByShift(Long workShiftId);
}
