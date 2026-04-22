package org.cts.factorypulse.operations.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.factorypulse.operations.model.*;
import org.cts.factorypulse.operations.repository.*;
import org.cts.factorypulse.operations.service.AlertService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertWorker {
    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final DowntimeEventRepository downtimeEventRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRate = 30000)
    public void evaluateAlertRules() {
        for (AlertRule rule : alertRuleRepository.findByActiveTrue()) {
            try { evaluateRule(rule); }
            catch (Exception e) { log.error("Alert rule evaluation failed for '{}': {}", rule.getName(), e.getMessage()); }
        }
    }

    private void evaluateRule(AlertRule rule) throws Exception {
        String expr = rule.getTriggerExpression().toLowerCase().trim();
        if (expr.startsWith("downtime >")) {
            int threshold = Integer.parseInt(expr.replace("downtime >", "").trim());
            for (DowntimeEvent dt : downtimeEventRepository.findByEndAtIsNull()) {
                long durationMin = Duration.between(dt.getStartAt(), LocalDateTime.now()).toMinutes();
                if (durationMin > threshold) {
                    boolean alreadyAlerted = alertRepository.existsByAlertRuleAndRelatedEntityIdAndStatus(rule, dt.getDowntimeId(), "OPEN");
                    if (!alreadyAlerted) {
                        Alert alert = alertService.triggerAlert(rule, dt.getDowntimeId(), "DOWNTIME", "OPEN",
                                String.format("Machine #%d has been down for %d minutes", dt.getMachineId(), durationMin));
                        notifyRecipients(rule, alert,
                                String.format("ALERT: Machine #%d on Line #%d has been down for %d minutes.", dt.getMachineId(), dt.getLineId(), durationMin));
                    }
                }
            }
        }
    }

    private void notifyRecipients(AlertRule rule, Alert alert, String message) throws Exception {
        if (rule.getRecipientsJson() == null || rule.getRecipientsJson().isBlank()) return;
        List<String> roles = objectMapper.readValue(rule.getRecipientsJson(), new TypeReference<>() {});
        for (String roleName : roles) {
            Role role = Role.valueOf(roleName);
            List<User> recipients = userRepository.findByRole(role).stream()
                    .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).toList();
            if (recipients.isEmpty()) { log.warn("No active {} users to notify.", roleName); continue; }
            for (User user : recipients) alertService.sendNotification(alert, user.getUserId(), message);
        }
    }
}
