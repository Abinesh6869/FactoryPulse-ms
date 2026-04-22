package org.cts.factorypulse.operations.service;

import lombok.RequiredArgsConstructor;
import org.cts.factorypulse.operations.model.AuditLog;
import org.cts.factorypulse.operations.model.User;
import org.cts.factorypulse.operations.repository.AuditLogRepository;
import org.cts.factorypulse.operations.repository.UserRepository;
import org.cts.factorypulse.operations.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(String action, String resource, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getUserId()).orElse(null);
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setResource(resource);
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
