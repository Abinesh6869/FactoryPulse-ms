package org.cts.factorypulse.operations.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class DowntimeEventResponse {
    private Long downtimeId;
    private Long lineId;
    private Long machineId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long durationSec;
    private String category;
    private Long rootCauseId;
    private String rootCauseCode;
    private String rootCauseDescription;
    private Long loggedBy;
    private String notes;
    private LocalDateTime createdAt;
    private String warning;
}
