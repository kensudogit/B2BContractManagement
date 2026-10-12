package com.example.contracts.approval;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.contracts.audit.AuditService;
import com.example.contracts.common.ApiException;
import com.example.contracts.contract.Contract;
import com.example.contracts.contract.ContractService;
import com.example.contracts.contract.ContractVersion;
import com.example.contracts.security.AppUser;
import com.example.contracts.security.AppUserRepository;

@Service
public class ApprovalService {
    private static final Set<String> OPEN = Set.of("REQUESTED", "IN_PROGRESS");

    private final ApprovalRequestRepository requests;
    private final ApprovalStepRepository steps;
    private final ApprovalHistoryRepository histories;
    private final AppUserRepository users;
    private final ContractService contracts;
    private final AuditService audit;

    public ApprovalService(
            ApprovalRequestRepository requests,
            ApprovalStepRepository steps,
            ApprovalHistoryRepository histories,
            AppUserRepository users,
            ContractService contracts,
            AuditService audit) {
        this.requests = requests;
        this.steps = steps;
        this.histories = histories;
        this.users = users;
        this.contracts = contracts;
        this.audit = audit;
    }

    @Transactional
    public ApprovalDtos.RequestView submit(Long contractId, ApprovalDtos.SubmitRequest req, String idempotencyKey, AppUser actor) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = requests.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return toView(existing.get());
            }
        }
        Contract contract = contracts.require(contractId);
        if (!"DRAFT".equals(contract.getStatus())) {
            throw ApiException.conflict("下書きの契約のみ承認申請できます");
        }
        if (requests.findFirstByContractIdAndStatusIn(contractId, List.copyOf(OPEN)).isPresent()) {
            throw ApiException.conflict("未完了の承認申請があります");
        }
        ContractVersion version = contracts.latestVersion(contractId);
        ApprovalRequest request = requests.save(ApprovalRequest.create(
                contractId, version.getId(), actor.getId(),
                idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim()));
        int order = 1;
        for (Long approverId : req.approverIds()) {
            AppUser approver = users.findById(approverId)
                    .orElseThrow(() -> ApiException.badRequest("承認者が存在しません"));
            if (!Set.of("APPROVER", "ADMIN", "LEGAL").contains(approver.getRole())) {
                throw ApiException.badRequest("承認ロールのないユーザーは指定できません");
            }
            steps.save(ApprovalStep.of(request.getId(), order++, approverId));
        }
        if (req.comment() != null && !req.comment().isBlank()) {
            histories.save(ApprovalHistory.of(request.getId(), actor.getId(), "SUBMIT", req.comment()));
        } else {
            histories.save(ApprovalHistory.of(request.getId(), actor.getId(), "SUBMIT", null));
        }
        contracts.markUnderReview(contract);
        audit.record(actor.getId(), "APPROVAL", request.getId(), "SUBMIT");
        return toView(request);
    }

    @Transactional
    public ApprovalDtos.RequestView approve(Long requestId, ApprovalDtos.DecisionRequest req, AppUser actor) {
        return decide(requestId, actor, "APPROVE", req == null ? null : req.comment());
    }

    @Transactional
    public ApprovalDtos.RequestView returnToDraft(Long requestId, ApprovalDtos.DecisionRequest req, AppUser actor) {
        return decide(requestId, actor, "RETURN", req == null ? null : req.comment());
    }

    @Transactional
    public ApprovalDtos.RequestView reject(Long requestId, ApprovalDtos.DecisionRequest req, AppUser actor) {
        return decide(requestId, actor, "REJECT", req == null ? null : req.comment());
    }

    @Transactional(readOnly = true)
    public ApprovalDtos.RequestView get(Long requestId) {
        return toView(require(requestId));
    }

    @Transactional(readOnly = true)
    public List<ApprovalDtos.RequestView> byContract(Long contractId) {
        return requests.findByContractIdOrderByRequestedAtDesc(contractId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalDtos.InboxItem> inbox(AppUser actor) {
        return steps.findByApproverIdAndStatus(actor.getId(), "PENDING").stream()
                .map(step -> {
                    ApprovalRequest request = require(step.getRequestId());
                    return new ApprovalDtos.InboxItem(
                            request.getId(), request.getContractId(), request.getVersionId(),
                            request.getStatus(), step.getStepOrder());
                })
                .filter(item -> OPEN.contains(item.requestStatus()))
                .toList();
    }

    private ApprovalDtos.RequestView decide(Long requestId, AppUser actor, String action, String comment) {
        ApprovalRequest request = require(requestId);
        if (!OPEN.contains(request.getStatus())) {
            throw ApiException.conflict("この申請は既に完了しています");
        }
        List<ApprovalStep> stepList = steps.findByRequestIdOrderByStepOrderAsc(requestId);
        ApprovalStep current = stepList.stream()
                .filter(s -> "PENDING".equals(s.getStatus()))
                .findFirst()
                .orElseThrow(() -> ApiException.conflict("処理待ちのステップがありません"));
        if (!current.getApproverId().equals(actor.getId()) && !"ADMIN".equals(actor.getRole())) {
            throw ApiException.forbidden("このステップの承認者ではありません");
        }
        if ("APPROVE".equals(action)) {
            current.setStatus("APPROVED");
            steps.save(current);
            histories.save(ApprovalHistory.of(requestId, actor.getId(), "APPROVE", comment));
            boolean remaining = stepList.stream()
                    .anyMatch(s -> !s.getId().equals(current.getId()) && "PENDING".equals(s.getStatus()));
            if (remaining) {
                request.setStatus("IN_PROGRESS");
            } else {
                request.setStatus("APPROVED");
                contracts.applyApprovalOutcome(contracts.require(request.getContractId()), "APPROVED");
            }
        } else if ("RETURN".equals(action)) {
            current.setStatus("RETURNED");
            steps.save(current);
            request.setStatus("RETURNED");
            histories.save(ApprovalHistory.of(requestId, actor.getId(), "RETURN", comment));
            contracts.applyApprovalOutcome(contracts.require(request.getContractId()), "RETURNED");
        } else if ("REJECT".equals(action)) {
            current.setStatus("REJECTED");
            steps.save(current);
            request.setStatus("REJECTED");
            histories.save(ApprovalHistory.of(requestId, actor.getId(), "REJECT", comment));
            contracts.applyApprovalOutcome(contracts.require(request.getContractId()), "REJECTED");
        } else {
            throw ApiException.badRequest("未知の承認操作です");
        }
        requests.save(request);
        audit.record(actor.getId(), "APPROVAL", request.getId(), action);
        return toView(request);
    }

    private ApprovalRequest require(Long id) {
        return requests.findById(id).orElseThrow(() -> ApiException.notFound("承認申請が見つかりません"));
    }

    private ApprovalDtos.RequestView toView(ApprovalRequest request) {
        List<ApprovalDtos.StepView> stepViews = steps.findByRequestIdOrderByStepOrderAsc(request.getId()).stream()
                .map(s -> new ApprovalDtos.StepView(s.getId(), s.getStepOrder(), s.getApproverId(), s.getStatus()))
                .toList();
        List<ApprovalDtos.HistoryView> historyViews = histories.findByRequestIdOrderByActedAtAsc(request.getId()).stream()
                .map(h -> new ApprovalDtos.HistoryView(h.getId(), h.getActorId(), h.getAction(), h.getCommentText(), h.getActedAt()))
                .toList();
        return new ApprovalDtos.RequestView(
                request.getId(), request.getContractId(), request.getVersionId(), request.getStatus(),
                request.getRequestedBy(), request.getRequestedAt(), stepViews, historyViews);
    }
}
