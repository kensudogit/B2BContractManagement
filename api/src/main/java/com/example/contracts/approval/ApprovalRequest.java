package com.example.contracts.approval;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(nullable = false)
    private String status = "REQUESTED";

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt = OffsetDateTime.now();

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    public Long getId() { return id; }
    public Long getContractId() { return contractId; }
    public Long getVersionId() { return versionId; }
    public String getStatus() { return status; }
    public Long getRequestedBy() { return requestedBy; }
    public OffsetDateTime getRequestedAt() { return requestedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }

    public void setStatus(String status) { this.status = status; }

    public static ApprovalRequest create(Long contractId, Long versionId, Long requestedBy, String idempotencyKey) {
        ApprovalRequest a = new ApprovalRequest();
        a.contractId = contractId;
        a.versionId = versionId;
        a.requestedBy = requestedBy;
        a.status = "REQUESTED";
        a.idempotencyKey = idempotencyKey;
        return a;
    }
}
