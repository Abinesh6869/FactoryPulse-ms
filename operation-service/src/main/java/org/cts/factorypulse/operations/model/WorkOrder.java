package org.cts.factorypulse.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class WorkOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workOrderId;
    @Column(name = "machine_id") private Long machineId;
    @ManyToOne @JoinColumn(name = "downtime_id")
    private DowntimeEvent downtimeEvent;
    @Column(name = "created_by") private Long createdById;
    private String priority;
    private String description;
    @Column(name = "assigned_to") private Long assignedToId;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
    private String status;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
