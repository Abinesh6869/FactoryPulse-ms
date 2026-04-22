package org.cts.factorypulse.operations.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Alert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;
    @ManyToOne @JoinColumn(name = "rule_id")
    private AlertRule alertRule;
    private String entityType;
    private Long relatedEntityId;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;
    private String status;
    private String notes;
}
