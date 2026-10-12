package com.example.contracts.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void record(Long actorId, String type, Object resourceId, String action) {
        repo.save(AuditLog.of(actorId, type, String.valueOf(resourceId), action));
    }
}
