package org.cts.factorypulse.operations.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cts.factorypulse.operations.dto.request.RootCauseRequest;
import org.cts.factorypulse.operations.dto.response.RootCauseResponse;
import org.cts.factorypulse.operations.exception.ApiResponse;
import org.cts.factorypulse.operations.security.UserPrincipal;
import org.cts.factorypulse.operations.service.RootCauseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rootcauses")
@RequiredArgsConstructor
public class RootCauseController {
    private final RootCauseService rootCauseService;

    @PostMapping
    public ResponseEntity<ApiResponse<RootCauseResponse>> createRootCause(
            @Valid @RequestBody RootCauseRequest request, Authentication authentication) {
        Long createdBy = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Root cause created successfully", rootCauseService.createRootCause(request, createdBy)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getRootCauses(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String category) {
        if (id != null) return ResponseEntity.ok(ApiResponse.success("Root cause fetched", rootCauseService.getRootCauseById(id)));
        if (category != null) return ResponseEntity.ok(ApiResponse.success("Root causes fetched", rootCauseService.getRootCausesByCategory(category)));
        return ResponseEntity.ok(ApiResponse.success("Root causes fetched", rootCauseService.getAllRootCauses()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RootCauseResponse>> updateRootCause(
            @PathVariable Long id, @Valid @RequestBody RootCauseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Root cause updated", rootCauseService.updateRootCause(id, request)));
    }
}
