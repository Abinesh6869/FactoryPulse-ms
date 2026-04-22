package org.cts.factorypulse.operations.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DowntimeEventRequest {
    @NotNull(message = "Line ID is required") private Long lineId;
    @NotNull(message = "Machine ID is required") private Long machineId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String category;
    private Long rootCauseId;
    private String notes;
}
