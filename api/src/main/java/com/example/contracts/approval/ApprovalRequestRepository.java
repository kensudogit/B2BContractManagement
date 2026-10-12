package com.example.contracts.approval;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByContractIdOrderByRequestedAtDesc(Long contractId);

    Optional<ApprovalRequest> findFirstByContractIdAndStatusIn(Long contractId, List<String> statuses);

    Optional<ApprovalRequest> findByIdempotencyKey(String idempotencyKey);
}
