package com.giapho.coffee_shop_backend.shift.messaging;

import com.giapho.coffee_shop_backend.dto.shift.ShiftReportResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftSessionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShiftRealtimePublisher {

    private static final String TOPIC_PREFIX = "/topic/shifts";
    private static final String SESSION_EVENTS_DESTINATION = TOPIC_PREFIX + "/session-events";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishSessionStarted(ShiftSessionResponseDTO session) {
        messagingTemplate.convertAndSend(SESSION_EVENTS_DESTINATION,
                new ShiftSessionEvent("SESSION_STARTED", session, null));
    }

    public void publishSessionEnded(ShiftSessionResponseDTO session, ShiftReportResponseDTO report) {
        messagingTemplate.convertAndSend(SESSION_EVENTS_DESTINATION,
                new ShiftSessionEvent("SESSION_ENDED", session, report));
    }

    public void publishSessionForced(ShiftSessionResponseDTO session, ShiftReportResponseDTO report) {
        messagingTemplate.convertAndSend(SESSION_EVENTS_DESTINATION,
                new ShiftSessionEvent("SESSION_FORCED", session, report));
    }

    public record ShiftSessionEvent(String eventType,
                                    ShiftSessionResponseDTO session,
                                    ShiftReportResponseDTO report) {
    }
}
