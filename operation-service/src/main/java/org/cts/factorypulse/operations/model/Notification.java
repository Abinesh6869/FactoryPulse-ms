package org.cts.factorypulse.operations.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;
    @Column(name = "user_id") private Long userId;
    @ManyToOne @JoinColumn(name = "alert_id")
    private Alert alert;
    private String channel;
    private String message;
    private LocalDateTime sentAt;
    private String status;
}
