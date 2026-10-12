package com.example.contracts.approval;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    List<ApprovalStep> findByRequestIdOrderByStepOrderAsc(Long requestId);

    List<ApprovalStep> findByApproverIdAndStatus(Long approverId, String status);
}
