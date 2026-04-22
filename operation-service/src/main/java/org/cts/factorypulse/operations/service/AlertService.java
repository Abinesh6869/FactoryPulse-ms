package org.cts.factorypulse.operations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.factorypulse.operations.dto.response.AlertResponse;
import org.cts.factorypulse.operations.dto.response.NotificationResponse;
import org.cts.factorypulse.operations.exception.ResourceNotFoundException;
import org.cts.factorypulse.operations.model.Alert;
import org.cts.factorypulse.operations.model.AlertRule;
import org.cts.factorypulse.operations.model.Notification;
import org.cts.factorypulse.operations.repository.AlertRepository;
import org.cts.factorypulse.operations.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertRepository alertRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    public Alert triggerAlert(AlertRule rule, Long relatedEntityId, String entityType, String status, String notes) {
        Alert alert = new Alert();
        alert.setAlertRule(rule);
        alert.setRelatedEntityId(relatedEntityId);
        alert.setEntityType(entityType);
        alert.setStatus(status);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setNotes(notes);
        log.info("Alert triggered: Rule='{}' Entity={}", rule.getName(), relatedEntityId);
        return alertRepository.save(alert);
    }

    public void sendNotification(Alert alert, Long userId, String message) {
        Notification notification = new Notification();
        notification.setAlert(alert);
        notification.setUserId(userId);
        notification.setChannel("IN_APP");
        notification.setMessage(message);
        notification.setStatus("SENT");
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
        log.info("Notification sent to user: {}", userId);
    }

    public AlertResponse resolveAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));
        alert.setResolvedAt(LocalDateTime.now());
        alert.setStatus("RESOLVED");
        auditLogService.log("RESOLVE_ALERT", "Alert", "Resolved alert ID: " + alertId);
        return toAlertResponse(alertRepository.save(alert));
    }

    public Page<AlertResponse> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable).map(this::toAlertResponse);
    }

    public List<AlertResponse> getAllByRule(Long ruleId) {
        return alertRepository.findByAlertRuleRuleId(ruleId).stream().map(this::toAlertResponse).collect(Collectors.toList());
    }

    public List<AlertResponse> getOpenAlerts() {
        return alertRepository.findByStatus("OPEN").stream().map(this::toAlertResponse).collect(Collectors.toList());
    }

    public Page<NotificationResponse> getNotificationsByUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable).map(this::toNotificationResponse);
    }

    public List<NotificationResponse> getUnreadNotificationsByUser(Long userId) {
        return notificationRepository.findByUserIdAndStatus(userId, "SENT").stream()
                .map(this::toNotificationResponse).collect(Collectors.toList());
    }

    public NotificationResponse markNotificationRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        n.setStatus("READ");
        return toNotificationResponse(notificationRepository.save(n));
    }

    private AlertResponse toAlertResponse(Alert a) {
        return AlertResponse.builder()
                .alertId(a.getAlertId()).ruleId(a.getAlertRule().getRuleId())
                .ruleName(a.getAlertRule().getName()).severity(a.getAlertRule().getSeverity())
                .relatedEntityId(a.getRelatedEntityId()).entityType(a.getEntityType())
                .triggeredAt(a.getTriggeredAt()).resolvedAt(a.getResolvedAt())
                .status(a.getStatus()).notes(a.getNotes()).build();
    }

    private NotificationResponse toNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId()).userId(n.getUserId())
                .alertId(n.getAlert() != null ? n.getAlert().getAlertId() : null)
                .channel(n.getChannel()).message(n.getMessage())
                .sentAt(n.getSentAt()).status(n.getStatus()).build();
    }
}
