package org.cts.factorypulse.operations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cts.factorypulse.operations.dto.request.CorrectiveActionRequest;
import org.cts.factorypulse.operations.dto.request.DowntimeEventRequest;
import org.cts.factorypulse.operations.dto.response.CorrectiveActionResponse;
import org.cts.factorypulse.operations.dto.response.DowntimeEventResponse;
import org.cts.factorypulse.operations.exception.BadRequestException;
import org.cts.factorypulse.operations.exception.ResourceNotFoundException;
import org.cts.factorypulse.operations.model.*;
import org.cts.factorypulse.operations.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DowntimeEventService {
    private final DowntimeEventRepository downtimeEventRepository;
    private final CorrectiveActionRepository correctiveActionRepository;
    private final RootCauseRepository rootCauseRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    public DowntimeEventResponse createDowntime(DowntimeEventRequest request, Long loggedById) {
        boolean alreadyDown = downtimeEventRepository.findByEndAtIsNull()
                .stream().anyMatch(d -> request.getMachineId().equals(d.getMachineId()));
        if (alreadyDown) throw new BadRequestException("Machine ID " + request.getMachineId() + " already has an active downtime.");
        if (request.getStartAt() != null && request.getStartAt().isAfter(LocalDateTime.now()))
            throw new BadRequestException("Start time cannot be in the future.");
        if (request.getStartAt() != null && request.getEndAt() != null && request.getEndAt().isBefore(request.getStartAt()))
            throw new BadRequestException("End time cannot be before start time.");

        DowntimeEvent event = new DowntimeEvent();
        event.setLineId(request.getLineId());
        event.setMachineId(request.getMachineId());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setCategory(request.getCategory());
        event.setNotes(request.getNotes());
        event.setLoggedById(loggedById);
        if (request.getEndAt() != null)
            event.setDurationSec(Duration.between(request.getStartAt(), request.getEndAt()).toSeconds());
        if (request.getRootCauseId() != null)
            event.setRootCause(rootCauseRepository.findById(request.getRootCauseId())
                    .orElseThrow(() -> new ResourceNotFoundException("RootCause not found: " + request.getRootCauseId())));

        DowntimeEvent saved = downtimeEventRepository.save(event);
        auditLogService.log("CREATE_DOWNTIME", "DowntimeEvent", "Created downtime ID:" + saved.getDowntimeId());
        return toDowntimeResponse(saved);
    }

    public Page<DowntimeEventResponse> getAllDowntimes(Pageable pageable) {
        return downtimeEventRepository.findAll(pageable).map(this::toDowntimeResponse);
    }

    public List<DowntimeEventResponse> getDowntimesByLine(Long lineId, LocalDateTime from, LocalDateTime to) {
        return downtimeEventRepository.findByLineIdAndStartAtBetweenOrderByStartAtDesc(lineId, from, to)
                .stream().map(this::toDowntimeResponse).collect(Collectors.toList());
    }

    public List<DowntimeEventResponse> getDowntimesByMachine(Long machineId, LocalDateTime from, LocalDateTime to) {
        return downtimeEventRepository.findByMachineIdAndStartAtBetweenOrderByStartAtDesc(machineId, from, to)
                .stream().map(this::toDowntimeResponse).collect(Collectors.toList());
    }

    public List<DowntimeEventResponse> getActiveDowntimes() {
        return downtimeEventRepository.findByEndAtIsNull().stream().map(this::toDowntimeResponse).collect(Collectors.toList());
    }

    public DowntimeEventResponse getDowntimeById(Long id) {
        return toDowntimeResponse(downtimeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Downtime not found: " + id)));
    }

    public DowntimeEventResponse closeDowntime(Long id, LocalDateTime endAt) {
        DowntimeEvent event = downtimeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Downtime not found: " + id));
        if (event.getEndAt() != null) throw new BadRequestException("Downtime ID " + id + " is already closed.");
        if (endAt.isBefore(event.getStartAt())) throw new BadRequestException("End time cannot be before start time.");
        event.setEndAt(endAt);
        event.setDurationSec(Duration.between(event.getStartAt(), endAt).toSeconds());
        auditLogService.log("CLOSE_DOWNTIME", "DowntimeEvent", "Closed downtime ID: " + id);
        return toDowntimeResponse(downtimeEventRepository.save(event));
    }

    public DowntimeEventResponse tagRootCause(Long id, Long rootCauseId) {
        DowntimeEvent event = downtimeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Downtime not found: " + id));
        String warning = null;
        if (event.getEndAt() != null) warning = "Downtime ID " + id + " is already closed.";
        if (event.getRootCause() != null)
            warning = (warning != null ? warning + " | " : "") + "Root cause already set ('" + event.getRootCause().getCode() + "'). It will be overwritten.";
        event.setRootCause(rootCauseRepository.findById(rootCauseId)
                .orElseThrow(() -> new ResourceNotFoundException("RootCause not found: " + rootCauseId)));
        auditLogService.log("TAG_ROOT_CAUSE", "DowntimeEvent", "Tagged root cause " + rootCauseId + " on downtime " + id);
        return toDowntimeResponse(downtimeEventRepository.save(event), warning);
    }

    public CorrectiveActionResponse createCorrectiveAction(CorrectiveActionRequest request) {
        DowntimeEvent downtime = downtimeEventRepository.findById(request.getDowntimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Downtime not found: " + request.getDowntimeId()));
        if (downtime.getEndAt() == null) throw new BadRequestException("Cannot create corrective action for an open downtime.");
        if (downtime.getRootCause() == null) throw new BadRequestException("Cannot create corrective action without a root cause.");
        if (request.getDueDate() != null && request.getDueDate().isBefore(java.time.LocalDate.now()))
            throw new BadRequestException("Due date cannot be in the past.");

        CorrectiveAction action = new CorrectiveAction();
        action.setDowntimeEvent(downtime);
        action.setAssignedToId(request.getAssignedTo());
        action.setDescription(request.getDescription());
        action.setDueDate(request.getDueDate());
        action.setStatus("OPEN");
        CorrectiveAction saved = correctiveActionRepository.save(action);

        Notification notification = new Notification();
        notification.setUserId(request.getAssignedTo());
        notification.setChannel("IN_APP");
        notification.setMessage("You have been assigned a Corrective Action: \"" + request.getDescription()
                + "\" | Root Cause: " + downtime.getRootCause().getCode() + " | Due: " + request.getDueDate());
        notification.setSentAt(LocalDateTime.now());
        notification.setStatus("UNREAD");
        notificationRepository.save(notification);

        auditLogService.log("CREATE_CORRECTIVE_ACTION", "CorrectiveAction", "Created corrective action ID: " + saved.getActionId());
        return toActionResponse(saved);
    }

    public List<CorrectiveActionResponse> getActionsByDowntime(Long downtimeId) {
        return correctiveActionRepository.findByDowntimeEventDowntimeId(downtimeId)
                .stream().map(this::toActionResponse).collect(Collectors.toList());
    }

    public CorrectiveActionResponse completeAction(Long actionId) {
        CorrectiveAction action = correctiveActionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Corrective action not found: " + actionId));
        if ("COMPLETED".equalsIgnoreCase(action.getStatus()))
            throw new BadRequestException("Corrective action ID " + actionId + " is already completed.");
        action.setStatus("COMPLETED");
        action.setCompletedAt(LocalDateTime.now());
        auditLogService.log("COMPLETE_CORRECTIVE_ACTION", "CorrectiveAction", "Completed action ID: " + actionId);
        return toActionResponse(correctiveActionRepository.save(action));
    }

    private DowntimeEventResponse toDowntimeResponse(DowntimeEvent e) { return toDowntimeResponse(e, null); }

    private DowntimeEventResponse toDowntimeResponse(DowntimeEvent e, String warning) {
        return DowntimeEventResponse.builder()
                .downtimeId(e.getDowntimeId()).lineId(e.getLineId()).machineId(e.getMachineId())
                .startAt(e.getStartAt()).endAt(e.getEndAt()).durationSec(e.getDurationSec())
                .category(e.getCategory())
                .rootCauseId(e.getRootCause() != null ? e.getRootCause().getRootCauseId() : null)
                .rootCauseCode(e.getRootCause() != null ? e.getRootCause().getCode() : null)
                .rootCauseDescription(e.getRootCause() != null ? e.getRootCause().getDescription() : null)
                .loggedBy(e.getLoggedById()).notes(e.getNotes()).createdAt(e.getCreatedAt()).warning(warning)
                .build();
    }

    private CorrectiveActionResponse toActionResponse(CorrectiveAction a) {
        return CorrectiveActionResponse.builder()
                .actionId(a.getActionId()).downtimeId(a.getDowntimeEvent().getDowntimeId())
                .assignedTo(a.getAssignedToId()).description(a.getDescription())
                .dueDate(a.getDueDate()).completedAt(a.getCompletedAt())
                .status(a.getStatus()).createdAt(a.getCreatedAt())
                .build();
    }
}
