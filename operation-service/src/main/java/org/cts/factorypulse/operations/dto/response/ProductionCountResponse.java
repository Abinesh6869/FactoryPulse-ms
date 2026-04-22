package org.cts.factorypulse.operations.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductionCountResponse {
    private Long countId;
    private Long lineId;
    private Long shiftId;
    private LocalDateTime timeStamp;
    private Long goodCount;
    private Long rejectCount;
}
