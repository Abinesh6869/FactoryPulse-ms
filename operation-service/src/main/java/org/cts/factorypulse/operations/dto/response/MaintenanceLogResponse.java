package org.cts.factorypulse.operations.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MaintenanceLogResponse {

    private Long logId;
    private Long workOrderId;
    private Long machineId;
    private Long performedBy;
    private LocalDateTime performedAt;
    private String notes;
    private List<PartUsed> partsUsedJson;
    private Integer timeSpentMinutes;
    private LocalDateTime createdAt;

    @Data
    public static class PartUsed {
        private String part;
        private String qty;
    }
}
