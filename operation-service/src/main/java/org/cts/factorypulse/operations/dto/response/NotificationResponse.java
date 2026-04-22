package org.cts.factorypulse.operations.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class NotificationResponse {
    private Long notificationId;
    private Long userId;
    private Long alertId;
    private String channel;
    private String message;
    private LocalDateTime sentAt;
    private String status;
}
