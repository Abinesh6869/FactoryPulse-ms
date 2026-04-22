package org.cts.factorypulse.operations.service;

import lombok.RequiredArgsConstructor;
import org.cts.factorypulse.operations.dto.request.RootCauseRequest;
import org.cts.factorypulse.operations.dto.response.RootCauseResponse;
import org.cts.factorypulse.operations.exception.ResourceNotFoundException;
import org.cts.factorypulse.operations.model.RootCause;
import org.cts.factorypulse.operations.model.User;
import org.cts.factorypulse.operations.repository.RootCauseRepository;
import org.cts.factorypulse.operations.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RootCauseService {
    private final RootCauseRepository rootCauseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public RootCauseResponse createRootCause(RootCauseRequest request, Long createdByUserId) {
        User user = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + createdByUserId));
        RootCause rc = new RootCause();
        rc.setCode(request.getCode());
        rc.setDescription(request.getDescription());
        rc.setCategory(request.getCategory());
        rc.setCreatedBy(user);
        RootCause saved = rootCauseRepository.save(rc);
        auditLogService.log("CREATE_ROOT_CAUSE", "RootCause", "Created root cause ID: " + saved.getRootCauseId());
        return toResponse(saved);
    }

    public List<RootCauseResponse> getAllRootCauses() {
        return rootCauseRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<RootCauseResponse> getRootCausesByCategory(String category) {
        return rootCauseRepository.findByCategory(category).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RootCauseResponse getRootCauseById(Long id) {
        return toResponse(rootCauseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RootCause not found: " + id)));
    }

    public RootCauseResponse updateRootCause(Long id, RootCauseRequest request) {
        RootCause rc = rootCauseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RootCause not found: " + id));
        rc.setCode(request.getCode());
        rc.setDescription(request.getDescription());
        rc.setCategory(request.getCategory());
        auditLogService.log("UPDATE_ROOT_CAUSE", "RootCause", "Updated root cause ID: " + id);
        return toResponse(rootCauseRepository.save(rc));
    }

    private RootCauseResponse toResponse(RootCause rc) {
        return RootCauseResponse.builder()
                .rootCauseId(rc.getRootCauseId())
                .code(rc.getCode())
                .description(rc.getDescription())
                .category(rc.getCategory())
                .createdBy(rc.getCreatedBy() != null ? rc.getCreatedBy().getUserId() : null)
                .createdByEmployeeId(rc.getCreatedBy() != null ? rc.getCreatedBy().getEmployeeId() : null)
                .createdByName(rc.getCreatedBy() != null ? rc.getCreatedBy().getUserName() : null)
                .createdAt(rc.getCreatedAt())
                .build();
    }
}
