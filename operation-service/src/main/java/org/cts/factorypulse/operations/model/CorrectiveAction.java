package org.cts.factorypulse.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CorrectiveAction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionId;
    @Column(name = "assigned_to") private Long assignedToId;
    private String description;
    @ManyToOne @JoinColumn(name = "downtime_id")
    private DowntimeEvent downtimeEvent;
    private LocalDate dueDate;
    @CreationTimestamp private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String status;
}
