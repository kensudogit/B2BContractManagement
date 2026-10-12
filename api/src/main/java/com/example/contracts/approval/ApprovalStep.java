package com.example.contracts.approval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_steps")
public class ApprovalStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_id")
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    @Column(nullable = false)
    private String status = "PENDING";

    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public Integer getStepOrder() { return stepOrder; }
    public Long getApproverId() { return approverId; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public static ApprovalStep of(Long requestId, int stepOrder, Long approverId) {
        ApprovalStep s = new ApprovalStep();
        s.requestId = requestId;
        s.stepOrder = stepOrder;
        s.approverId = approverId;
        s.status = "PENDING";
        return s;
    }
}
