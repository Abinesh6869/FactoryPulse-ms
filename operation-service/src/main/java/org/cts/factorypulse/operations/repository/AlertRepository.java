package org.cts.factorypulse.operations.repository;

import org.cts.factorypulse.operations.model.Alert;
import org.cts.factorypulse.operations.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByAlertRuleRuleId(Long ruleId);
    List<Alert> findByStatus(String status);
    boolean existsByAlertRuleAndRelatedEntityIdAndStatus(AlertRule alertRule, Long relatedEntityId, String status);
}
