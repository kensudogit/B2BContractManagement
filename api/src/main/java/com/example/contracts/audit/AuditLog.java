package com.example.contracts.audit;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(nullable = false)
    private String action;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt = OffsetDateTime.now();

    @Column(name = "request_id")
    private String requestId;

    public static AuditLog of(Long actorId, String type, String resourceId, String action) {
        AuditLog log = new AuditLog();
        log.actorId = actorId;
        log.resourceType = type;
        log.resourceId = resourceId;
        log.action = action;
        return log;
    }
}
