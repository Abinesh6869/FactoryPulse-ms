package org.cts.factorypulse.operations.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductionCount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long countId;
    @Column(name = "line_id") private Long lineId;
    @Column(name = "shift_id") private Long shiftId;
    private LocalDateTime timeStamp;
    private Long goodCount;
    private Long rejectCount;
}
