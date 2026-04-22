package org.cts.factorypulse.operations.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.cts.factorypulse.operations.dto.request.AlertRuleRequest;
import org.cts.factorypulse.operations.dto.response.AlertRuleResponse;
import org.cts.factorypulse.operations.exception.ResourceNotFoundException;
import org.cts.factorypulse.operations.model.AlertRule;
import org.cts.factorypulse.operations.repository.AlertRuleRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertRuleService {
    private final AlertRuleRepository alertRuleRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AlertRuleResponse createAlertRule(AlertRuleRequest request) throws Exception {
        AlertRule rule = new AlertRule();
        rule.setName(request.getName());
        rule.setTriggerExpression(request.getTriggerExpression());
        rule.setSeverity(request.getSeverity());
        rule.setRecipientsJson(objectMapper.writeValueAsString(request.getRecipientsJson()));
        rule.setActive(request.getActive());
        AlertRule saved = alertRuleRepository.save(rule);
        auditLogService.log("CREATE_ALERT_RULE", "AlertRule", "Created alert rule: " + saved.getName());
        return toResponse(saved);
    }

    public List<AlertRuleResponse> getAllAlertRules() {
        return alertRuleRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<AlertRuleResponse> getActiveAlertRules() {
        return alertRuleRepository.findByActiveTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AlertRuleResponse getAlertRuleById(Long id) {
        return toResponse(alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + id)));
    }

    public AlertRuleResponse updateAlertRule(Long id, AlertRuleRequest request) throws Exception {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + id));
        rule.setName(request.getName());
        rule.setTriggerExpression(request.getTriggerExpression());
        rule.setSeverity(request.getSeverity());
        rule.setRecipientsJson(objectMapper.writeValueAsString(request.getRecipientsJson()));
        rule.setActive(request.getActive());
        auditLogService.log("UPDATE_ALERT_RULE", "AlertRule", "Updated alert rule ID: " + id);
        return toResponse(alertRuleRepository.save(rule));
    }

    public AlertRuleResponse toggleActive(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + id));
        rule.setActive(!rule.getActive());
        auditLogService.log("TOGGLE_ALERT_RULE", "AlertRule", "Toggled alert rule ID: " + id);
        return toResponse(alertRuleRepository.save(rule));
    }

    public void deleteAlertRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert rule not found: " + id));
        alertRuleRepository.delete(rule);
        auditLogService.log("DELETE_ALERT_RULE", "AlertRule", "Deleted alert rule ID: " + id);
    }

    private AlertRuleResponse toResponse(AlertRule rule) {
        List<String> recipients = new ArrayList<>();
        try {
            if (rule.getRecipientsJson() != null && !rule.getRecipientsJson().isBlank())
                recipients = objectMapper.readValue(rule.getRecipientsJson(), new TypeReference<>() {});
        } catch (Exception ignored) {}
        return AlertRuleResponse.builder().ruleId(rule.getRuleId()).name(rule.getName())
                .triggerExpression(rule.getTriggerExpression()).severity(rule.getSeverity())
                .recipientsJson(recipients).active(rule.getActive()).build();
    }
}
