package org.cts.factorypulse.operations.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.factorypulse.operations.dto.request.WorkOrderRequest;
import org.cts.factorypulse.operations.dto.response.PageResponse;
import org.cts.factorypulse.operations.dto.response.WorkOrderResponse;
import org.cts.factorypulse.operations.exception.ApiResponse;
import org.cts.factorypulse.operations.security.UserPrincipal;
import org.cts.factorypulse.operations.service.WorkOrderService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/workorders")
@RequiredArgsConstructor
public class WorkOrderController {
    private final WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkOrderResponse>> createWorkOrder(
            @Valid @RequestBody WorkOrderRequest request, Authentication authentication) {
        Long createdBy = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Work order created", workOrderService.createWorkOrder(request, createdBy)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getWorkOrders(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long machineId,
            @PageableDefault(size = 10, sort = "workOrderId", direction = Sort.Direction.ASC) Pageable pageable) {
        if (id != null) return ResponseEntity.ok(ApiResponse.success("Work order fetched", workOrderService.getWorkOrderById(id)));
        if (status != null) return ResponseEntity.ok(ApiResponse.success("Work orders fetched", workOrderService.getWorkOrdersByStatus(status)));
        if (machineId != null) return ResponseEntity.ok(ApiResponse.success("Work orders fetched", workOrderService.getWorkOrdersByMachine(machineId)));
        return ResponseEntity.ok(ApiResponse.success("Work orders fetched", new PageResponse<>(workOrderService.getAllWorkOrders(pageable))));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", workOrderService.updateStatus(id, status)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<WorkOrderResponse>>> getMyWorkOrders(Authentication authentication) {
        Long technicianId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.ok(ApiResponse.success("Your work orders fetched", workOrderService.getMyWorkOrders(technicianId)));
    }

    @PatchMapping("/{id}/reassign")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> reassign(@PathVariable Long id, @RequestParam Long technicianId) {
        return ResponseEntity.ok(ApiResponse.success("Work order reassigned", workOrderService.reassign(id, technicianId)));
    }
}
