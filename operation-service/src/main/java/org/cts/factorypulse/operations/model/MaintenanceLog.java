package org.cts.factorypulse.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MaintenanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne
    @JoinColumn(name = "workOrder_id")
    private WorkOrder workOrder;

    @Column(name = "performed_by")
    private Long performedById;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime performedAt;

    private String notes;

    @Column(name = "parts_used_json", columnDefinition = "TEXT")
    private String partsUsedJson;

    private Integer timeSpentMinutes;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
