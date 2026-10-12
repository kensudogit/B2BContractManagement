package com.example.contracts.approval;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public final class ApprovalDtos {
    private ApprovalDtos() {}

    public record SubmitRequest(@NotEmpty List<Long> approverIds, String comment) {}

    public record DecisionRequest(String comment) {}

    public record StepView(Long id, Integer stepOrder, Long approverId, String status) {}

    public record HistoryView(Long id, Long actorId, String action, String commentText, OffsetDateTime actedAt) {}

    public record RequestView(
            Long id,
            Long contractId,
            Long versionId,
            String status,
            Long requestedBy,
            OffsetDateTime requestedAt,
            List<StepView> steps,
            List<HistoryView> histories
    ) {}

    public record InboxItem(Long requestId, Long contractId, Long versionId, String requestStatus, Integer stepOrder) {}
}
