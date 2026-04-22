package org.cts.factorypulse.operations.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.factorypulse.operations.dto.request.CorrectiveActionRequest;
import org.cts.factorypulse.operations.dto.request.DowntimeEventRequest;
import org.cts.factorypulse.operations.dto.response.*;
import org.cts.factorypulse.operations.exception.ApiResponse;
import org.cts.factorypulse.operations.security.UserPrincipal;
import org.cts.factorypulse.operations.service.DowntimeEventService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/downtimes")
@RequiredArgsConstructor
public class DowntimeController {
    private final DowntimeEventService downtimeService;

    @PostMapping
    public ResponseEntity<ApiResponse<DowntimeEventResponse>> createDowntime(
            @Valid @RequestBody DowntimeEventRequest request, Authentication authentication) {
        Long loggedBy = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Downtime created successfully", downtimeService.createDowntime(request, loggedBy)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getDowntimes(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        if (id != null) return ResponseEntity.ok(ApiResponse.success("Downtime fetched", downtimeService.getDowntimeById(id)));
        if (Boolean.TRUE.equals(active)) return ResponseEntity.ok(ApiResponse.success("Active downtimes fetched", downtimeService.getActiveDowntimes()));
        if (lineId != null) return ResponseEntity.ok(ApiResponse.success("Downtimes fetched", downtimeService.getDowntimesByLine(lineId, from, to)));
        if (machineId != null) return ResponseEntity.ok(ApiResponse.success("Downtimes fetched", downtimeService.getDowntimesByMachine(machineId, from, to)));
        return ResponseEntity.ok(ApiResponse.success("Downtimes fetched", new PageResponse<>(downtimeService.getAllDowntimes(pageable))));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<DowntimeEventResponse>> closeDowntime(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt) {
        return ResponseEntity.ok(ApiResponse.success("Downtime closed", downtimeService.closeDowntime(id, endAt)));
    }

    @PatchMapping("/{id}/rootcause")
    public ResponseEntity<ApiResponse<DowntimeEventResponse>> tagRootCause(
            @PathVariable Long id, @RequestParam Long rootCauseId) {
        return ResponseEntity.ok(ApiResponse.success("Root cause tagged", downtimeService.tagRootCause(id, rootCauseId)));
    }

    @PostMapping("/actions")
    public ResponseEntity<ApiResponse<CorrectiveActionResponse>> createCorrectiveAction(
            @Valid @RequestBody CorrectiveActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Corrective action created", downtimeService.createCorrectiveAction(request)));
    }

    @GetMapping("/{downtimeId}/actions")
    public ResponseEntity<ApiResponse<List<CorrectiveActionResponse>>> getActionsByDowntime(@PathVariable Long downtimeId) {
        return ResponseEntity.ok(ApiResponse.success("Corrective actions fetched", downtimeService.getActionsByDowntime(downtimeId)));
    }

    @PatchMapping("/actions/{actionId}/complete")
    public ResponseEntity<ApiResponse<CorrectiveActionResponse>> completeAction(@PathVariable Long actionId) {
        return ResponseEntity.ok(ApiResponse.success("Corrective action completed", downtimeService.completeAction(actionId)));
    }
}
