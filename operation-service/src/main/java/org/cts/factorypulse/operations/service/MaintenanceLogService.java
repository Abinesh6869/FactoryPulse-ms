package org.cts.factorypulse.operations.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.cts.factorypulse.operations.dto.request.MaintenanceLogRequest;
import org.cts.factorypulse.operations.dto.response.MaintenanceLogResponse;
import org.cts.factorypulse.operations.exception.BadRequestException;
import org.cts.factorypulse.operations.exception.ResourceNotFoundException;
import org.cts.factorypulse.operations.model.DowntimeEvent;
import org.cts.factorypulse.operations.model.MaintenanceLog;
import org.cts.factorypulse.operations.model.WorkOrder;
import org.cts.factorypulse.operations.repository.DowntimeEventRepository;
import org.cts.factorypulse.operations.repository.MaintenanceLogRepository;
import org.cts.factorypulse.operations.repository.WorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceLogService {

    private final MaintenanceLogRepository maintenanceLogRepository;
    private final WorkOrderRepository workOrderRepository;
    private final DowntimeEventRepository downtimeEventRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public MaintenanceLogResponse createLog(MaintenanceLogRequest request, Long performedById)
            throws JsonProcessingException {

        WorkOrder wo = workOrderRepository.findById(request.getWorkOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "WorkOrder not found: " + request.getWorkOrderId()));

        if ("COMPLETED".equalsIgnoreCase(wo.getStatus()))
            throw new BadRequestException(
                    "Cannot log against an already completed work order: " + request.getWorkOrderId());

        MaintenanceLog log = new MaintenanceLog();
        log.setWorkOrder(wo);
        log.setPerformedById(performedById);
        log.setNotes(request.getNotes());
        log.setPartsUsedJson(objectMapper.writeValueAsString(request.getPartsUsedJson()));
        log.setTimeSpentMinutes(request.getTimeSpentMinutes());

        // Auto-complete the work order
        wo.setStatus("COMPLETED");
        workOrderRepository.save(wo);
        auditLogService.log("COMPLETE_WORK_ORDER", "WorkOrder",
                "Auto-completed work order ID: " + wo.getWorkOrderId());

        // Auto-close linked open downtime if present
        DowntimeEvent downtime = wo.getDowntimeEvent();
        if (downtime != null && downtime.getEndAt() == null) {
            LocalDateTime now = LocalDateTime.now();
            downtime.setEndAt(now);
            downtime.setDurationSec(
                    java.time.Duration.between(downtime.getStartAt(), now).toSeconds());
            downtimeEventRepository.save(downtime);
            auditLogService.log("AUTO_CLOSE_DOWNTIME", "DowntimeEvent",
                    "Auto-closed downtime ID: " + downtime.getDowntimeId());
        }

        MaintenanceLog created = maintenanceLogRepository.save(log);
        auditLogService.log("CREATE_MAINTENANCE_LOG", "MaintenanceLog",
                "Created maintenance log ID: " + created.getLogId()
                        + " for work order: " + request.getWorkOrderId());
        return toResponse(created);
    }

    public Page<MaintenanceLogResponse> getAllLogs(Pageable pageable) {
        return maintenanceLogRepository.findAll(pageable).map(this::toResponse);
    }

    public List<MaintenanceLogResponse> getLogsByWorkOrder(Long workOrderId) {
        workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + workOrderId));
        return maintenanceLogRepository.findByWorkOrderWorkOrderId(workOrderId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MaintenanceLogResponse> getLogsByMachine(Long machineId) {
        return maintenanceLogRepository.findByWorkOrderMachineId(machineId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public MaintenanceLogResponse getLogById(Long id) {
        return toResponse(maintenanceLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MaintenanceLog not found: " + id)));
    }

    private MaintenanceLogResponse toResponse(MaintenanceLog log) {
        List<MaintenanceLogResponse.PartUsed> parts = new ArrayList<>();
        try {
            if (log.getPartsUsedJson() != null && !log.getPartsUsedJson().isBlank()) {
                parts = objectMapper.readValue(log.getPartsUsedJson(),
                        new TypeReference<List<MaintenanceLogResponse.PartUsed>>() {});
            }
        } catch (Exception e) {
            System.out.println("Error parsing parts JSON for log: " + log.getLogId());
        }
        return MaintenanceLogResponse.builder()
                .logId(log.getLogId())
                .workOrderId(log.getWorkOrder().getWorkOrderId())
                .machineId(log.getWorkOrder().getMachineId())
                .performedBy(log.getPerformedById())
                .performedAt(log.getPerformedAt())
                .notes(log.getNotes())
                .partsUsedJson(parts)
                .timeSpentMinutes(log.getTimeSpentMinutes())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
