package org.cts.factorypulse.operations.controller;

import lombok.RequiredArgsConstructor;
import org.cts.factorypulse.operations.dto.response.DowntimeEventResponse;
import org.cts.factorypulse.operations.dto.response.ProductionCountResponse;
import org.cts.factorypulse.operations.dto.response.WorkOrderResponse;
import org.cts.factorypulse.operations.exception.ResourceNotFoundException;
import org.cts.factorypulse.operations.model.DowntimeEvent;
import org.cts.factorypulse.operations.model.ProductionCount;
import org.cts.factorypulse.operations.model.WorkOrder;
import org.cts.factorypulse.operations.repository.DowntimeEventRepository;
import org.cts.factorypulse.operations.repository.ProductionCountRepository;
import org.cts.factorypulse.operations.repository.WorkOrderRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal REST API — accessible only with the X-Service-Key header.
 * Used by FactoryPulse (Analytics) service for cross-service data access.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final DowntimeEventRepository downtimeEventRepository;
    private final ProductionCountRepository productionCountRepository;
    private final WorkOrderRepository workOrderRepository;

    // ── DOWNTIME EVENTS ──────────────────────────────────────────────────────

    /** List by line + time range (OEE, Report, Quality) */
    @GetMapping("/downtimes")
    public List<DowntimeEventResponse> getDowntimes(
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<DowntimeEvent> events;
        if (lineId != null && from != null && to != null) {
            events = downtimeEventRepository.findByLineIdAndStartAtBetweenOrderByStartAtDesc(lineId, from, to);
        } else if (from != null && to != null) {
            events = downtimeEventRepository.findByStartAtBetween(from, to);
        } else {
            events = downtimeEventRepository.findAll();
        }
        return events.stream().map(this::toDowntimeResponse).collect(Collectors.toList());
    }

    /** Single downtime by ID */
    @GetMapping("/downtimes/{id}")
    public DowntimeEventResponse getDowntimeById(@PathVariable Long id) {
        return toDowntimeResponse(downtimeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Downtime not found: " + id)));
    }

    /** Auto-close a downtime (called by MaintenanceLogService when a work order is completed) */
    @PatchMapping("/downtimes/{id}/close")
    public DowntimeEventResponse closeDowntime(@PathVariable Long id) {
        DowntimeEvent event = downtimeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Downtime not found: " + id));
        if (event.getEndAt() != null) {
            return toDowntimeResponse(event); // already closed — silently skip
        }
        LocalDateTime now = LocalDateTime.now();
        event.setEndAt(now);
        event.setDurationSec(java.time.Duration.between(event.getStartAt(), now).toSeconds());
        return toDowntimeResponse(downtimeEventRepository.save(event));
    }

    // ── PRODUCTION COUNTS ────────────────────────────────────────────────────

    /** List by lineId + time range, OR by shiftId, OR by time range only (for KPI) */
    @GetMapping("/production-counts")
    public List<ProductionCountResponse> getProductionCounts(
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) Long shiftId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<ProductionCount> counts;
        if (shiftId != null) {
            counts = productionCountRepository.findByShiftId(shiftId);
        } else if (lineId != null && from != null && to != null) {
            counts = productionCountRepository.findByLineIdAndTimeStampBetweenOrderByTimeStampDesc(lineId, from, to);
        } else if (from != null && to != null) {
            counts = productionCountRepository.findByTimeStampBetween(from, to);
        } else {
            counts = productionCountRepository.findAll();
        }
        return counts.stream().map(this::toProductionCountResponse).collect(Collectors.toList());
    }

    /** Single production count by ID */
    @GetMapping("/production-counts/{id}")
    public ProductionCountResponse getProductionCountById(@PathVariable Long id) {
        return toProductionCountResponse(productionCountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionCount not found: " + id)));
    }

    // ── WORK ORDERS ──────────────────────────────────────────────────────────

    /** List by time range (KPI) */
    @GetMapping("/work-orders")
    public List<WorkOrderResponse> getWorkOrders(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<WorkOrder> orders;
        if (from != null && to != null) {
            orders = workOrderRepository.findByCreatedAtBetween(from, to);
        } else {
            orders = workOrderRepository.findAll();
        }
        return orders.stream().map(this::toWorkOrderResponse).collect(Collectors.toList());
    }

    /** Single work order by ID */
    @GetMapping("/work-orders/{id}")
    public WorkOrderResponse getWorkOrderById(@PathVariable Long id) {
        return toWorkOrderResponse(workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id)));
    }

    /** Mark a work order as COMPLETED (called by MaintenanceLogService) */
    @PatchMapping("/work-orders/{id}/complete")
    public WorkOrderResponse completeWorkOrder(@PathVariable Long id) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found: " + id));
        wo.setStatus("COMPLETED");
        return toWorkOrderResponse(workOrderRepository.save(wo));
    }

    // ── MAPPERS ──────────────────────────────────────────────────────────────

    private DowntimeEventResponse toDowntimeResponse(DowntimeEvent e) {
        return DowntimeEventResponse.builder()
                .downtimeId(e.getDowntimeId())
                .lineId(e.getLineId())
                .machineId(e.getMachineId())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .durationSec(e.getDurationSec())
                .category(e.getCategory())
                .rootCauseId(e.getRootCause() != null ? e.getRootCause().getRootCauseId() : null)
                .rootCauseCode(e.getRootCause() != null ? e.getRootCause().getCode() : null)
                .rootCauseDescription(e.getRootCause() != null ? e.getRootCause().getDescription() : null)
                .loggedBy(e.getLoggedById())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private ProductionCountResponse toProductionCountResponse(ProductionCount pc) {
        return ProductionCountResponse.builder()
                .countId(pc.getCountId())
                .lineId(pc.getLineId())
                .shiftId(pc.getShiftId())
                .timeStamp(pc.getTimeStamp())
                .goodCount(pc.getGoodCount())
                .rejectCount(pc.getRejectCount())
                .build();
    }

    private WorkOrderResponse toWorkOrderResponse(WorkOrder wo) {
        return WorkOrderResponse.builder()
                .workOrderId(wo.getWorkOrderId())
                .machineId(wo.getMachineId())
                .downtimeId(wo.getDowntimeEvent() != null ? wo.getDowntimeEvent().getDowntimeId() : null)
                .createdBy(wo.getCreatedById())
                .priority(wo.getPriority())
                .description(wo.getDescription())
                .assignedToId(wo.getAssignedToId())
                .status(wo.getStatus())
                .createdAt(wo.getCreatedAt())
                .updatedAt(wo.getUpdatedAt())
                .build();
    }
}
