package org.cts.factorypulse.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DowntimeEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long downtimeId;
    @Column(name = "line_id") private Long lineId;
    @Column(name = "machine_id") private Long machineId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long durationSec;
    private String category;
    @ManyToOne @JoinColumn(name = "rootcause_id")
    private RootCause rootCause;
    @Column(name = "logged_by") private Long loggedById;
    @CreationTimestamp private LocalDateTime createdAt;
    private String notes;
}
