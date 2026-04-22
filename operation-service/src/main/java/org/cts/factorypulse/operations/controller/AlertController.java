package org.cts.factorypulse.operations.controller;

import lombok.RequiredArgsConstructor;
import org.cts.factorypulse.operations.dto.response.AlertResponse;
import org.cts.factorypulse.operations.dto.response.NotificationResponse;
import org.cts.factorypulse.operations.dto.response.PageResponse;
import org.cts.factorypulse.operations.exception.ApiResponse;
import org.cts.factorypulse.operations.security.UserPrincipal;
import org.cts.factorypulse.operations.service.AlertService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {
    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAlerts(
            @RequestParam(required = false) Boolean open,
            @RequestParam(required = false) Long ruleId,
            @PageableDefault(size = 10, sort = "alertId", direction = Sort.Direction.ASC) Pageable pageable) {
        if (open != null && open) return ResponseEntity.ok(ApiResponse.success("Open alerts fetched", alertService.getOpenAlerts()));
        if (ruleId != null) return ResponseEntity.ok(ApiResponse.success("Alerts fetched", alertService.getAllByRule(ruleId)));
        return ResponseEntity.ok(ApiResponse.success("Alerts fetched", new PageResponse<>(alertService.getAllAlerts(pageable))));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Alert resolved", alertService.resolveAlert(id)));
    }

    @GetMapping("/notifications/me")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "notificationId", direction = Sort.Direction.ASC) Pageable pageable) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", new PageResponse<>(alertService.getNotificationsByUser(userId, pageable))));
    }

    @GetMapping("/notifications/me/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(Authentication authentication) {
        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", alertService.getUnreadNotificationsByUser(userId)));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", alertService.markNotificationRead(id)));
    }
}
