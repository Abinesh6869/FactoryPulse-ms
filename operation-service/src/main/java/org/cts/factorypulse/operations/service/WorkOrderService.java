package org.cts.factorypulse.operations.service;

import lombok.RequiredArgsConstructor;
import org.cts.factorypulse.operations.dto.request.WorkOrderRequest;
import org.cts.factorypulse.operations.dto.response.WorkOrderResponse;
import org.cts.factorypulse.operations.exception.BadRequestException;
import org.cts.factorypulse.operations.exception.ResourceNotFoundException;
import org.cts.factorypulse.operations.model.*;
import org.cts.factorypulse.operations.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final DowntimeEventRepository downtimeEventRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    public WorkOrderResponse createWorkOrder(WorkOrderRequest request, Long createdById) {
        WorkOrder wo = new WorkOrder();
        wo.setMachineId(request.getMachineId());
        wo.setCreatedById(createdById);
        wo.setAssignedToId(request.getAssignedToUserId());
        wo.setPriority(request.getPriority());
        wo.setDescription(request.getDescription());
        wo.setStatus("OPEN");

        if (request.getDowntimeId() != null) {
            DowntimeEvent downtime = downtimeEventRepository.findById(request.getDowntimeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Downtime not found: " + request.getDowntimeId()));
            if (downtime.getEndAt() != null)
                throw new BadRequestException("Cannot create a work order for a closed downtime ID: " + request.getDowntimeId());
            if (workOrderRepository.existsByDowntimeEventDowntimeId(downtime.getDowntimeId()))
                throw new BadRequestException("A work order already exists for downtime ID: " + request.getDowntimeId());
            wo.setDowntimeEvent(downtime);
        }

        List<WorkOrder> activeJobs = workOrderRepository.findByAssignedToIdAndStatusIn(
                request.getAssignedToUserId(), List.of("OPEN", "IN_PROGRESS"));
        String warning = null;
        if (!activeJobs.isEmpty()) {
            WorkOrder active = activeJobs.get(0);
            warning = "Technician already has " + activeJobs.size() + " active job(s). Active Work Order #"
                    + active.getWorkOrderId() + " on machine ID: " + active.getMachineId() + " (Status: " + active.getStatus() + ").";
        }

        WorkOrder created = workOrderRepository.save(wo);

        Notification notification = new Notification();
        notification.setUserId(request.getAssignedToUserId());
        notification.setChannel("IN_APP");
        notification.setMessage("You have been assigned Work Order: \"" + wo.getDescription()
                + "\" on machine ID: " + request.getMachineId() + " | Priority: " + wo.getPriority());
        notification.setSentAt(LocalDateTime.now());
        notification.setStatus("UNREAD");
        notificationRepository.save(notification);

        auditLogService.log("CREATE_WORK_ORDER", "WorkOrder", "Created work order ID: " + created.getWorkOrderId());
        return toResponse(created, warning);
    }

    public Page<WorkOrderResponse> getAllWorkOrders(Pageable pageable) {
        return workOrderRepository.findAll(pageable).map(this::toResponse);
    }

    public List<WorkOrderResponse> getWorkOrdersByStatus(String status) {
        return workOrderRepository.findByStatus(status).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<WorkOrderResponse> getWorkOrdersByMachine(Long machineId) {
        return workOrderRepository.findByMachineId(machineId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public WorkOrderResponse getWorkOrderById(Long id) {
        return toResponse(workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id)));
    }

    public WorkOrderResponse updateStatus(Long id, String status) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id));
        if (!List.of("OPEN", "IN_PROGRESS", "COMPLETED").contains(status.toUpperCase()))
            throw new BadRequestException("Invalid status: '" + status + "'. Allowed: OPEN, IN_PROGRESS, COMPLETED.");
        if ("COMPLETED".equalsIgnoreCase(wo.getStatus()) && !status.equalsIgnoreCase("COMPLETED"))
            throw new BadRequestException("Cannot reopen a completed work order ID: " + id);
        wo.setStatus(status.toUpperCase());
        auditLogService.log("UPDATE_WORK_ORDER_STATUS", "WorkOrder", "Work order " + id + " → " + status);
        return toResponse(workOrderRepository.save(wo));
    }

    public List<WorkOrderResponse> getMyWorkOrders(Long technicianId) {
        return workOrderRepository.findByAssignedToId(technicianId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public WorkOrderResponse reassign(Long workOrderId, Long newTechnicianId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + workOrderId));
        if ("COMPLETED".equalsIgnoreCase(wo.getStatus()))
            throw new BadRequestException("Cannot reassign a completed work order ID: " + workOrderId);
        wo.setAssignedToId(newTechnicianId);

        Notification notification = new Notification();
        notification.setUserId(newTechnicianId);
        notification.setChannel("IN_APP");
        notification.setMessage("You have been reassigned Work Order: \"" + wo.getDescription()
                + "\" on machine ID: " + wo.getMachineId() + " | Priority: " + wo.getPriority());
        notification.setSentAt(LocalDateTime.now());
        notification.setStatus("UNREAD");
        notificationRepository.save(notification);

        auditLogService.log("REASSIGN_WORK_ORDER", "WorkOrder", "Work order " + workOrderId + " reassigned to " + newTechnicianId);
        return toResponse(workOrderRepository.save(wo));
    }

    private WorkOrderResponse toResponse(WorkOrder wo) { return toResponse(wo, null); }

    private WorkOrderResponse toResponse(WorkOrder wo, String warning) {
        return WorkOrderResponse.builder()
                .workOrderId(wo.getWorkOrderId()).machineId(wo.getMachineId())
                .downtimeId(wo.getDowntimeEvent() != null ? wo.getDowntimeEvent().getDowntimeId() : null)
                .createdBy(wo.getCreatedById()).priority(wo.getPriority()).description(wo.getDescription())
                .status(wo.getStatus()).assignedToId(wo.getAssignedToId())
                .createdAt(wo.getCreatedAt()).updatedAt(wo.getUpdatedAt()).warning(warning)
                .build();
    }
}
