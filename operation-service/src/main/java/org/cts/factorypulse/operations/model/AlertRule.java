package org.cts.factorypulse.operations.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AlertRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ruleId;
    private String name;
    private String triggerExpression;
    private String severity;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipients_json", columnDefinition = "TEXT")
    private String recipientsJson;
    private Boolean active;
}
