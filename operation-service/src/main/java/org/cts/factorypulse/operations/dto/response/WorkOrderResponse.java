package org.cts.factorypulse.operations.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class WorkOrderResponse {
    private Long workOrderId;
    private Long machineId;
    private Long downtimeId;
    private Long createdBy;
    private String priority;
    private String description;
    private Long assignedToId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String warning;
}
