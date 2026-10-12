package com.example.contracts.approval;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {
    List<ApprovalHistory> findByRequestIdOrderByActedAtAsc(Long requestId);
}
