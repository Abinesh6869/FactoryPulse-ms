package org.cts.factorypulse.operations.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class CorrectiveActionResponse {
    private Long actionId;
    private Long downtimeId;
    private Long assignedTo;
    private String description;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private String status;
    private LocalDateTime createdAt;
}
