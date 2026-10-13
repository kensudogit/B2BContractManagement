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
        contracts.requireReadyForApproval(contract);
        if (requests.findFirstByContractIdAndStatusIn(contractId, List.copyOf(OPEN)).isPresent()) {
            throw ApiException.conflict("未完了の承認申請があります");
        }
        if (req == null || req.approverIds() == null || req.approverIds().isEmpty()) {
            throw ApiException.badRequest("承認者を1人以上指定してください");
        }
        if (req.approverIds().stream().distinct().count() != req.approverIds().size()) {
            throw ApiException.badRequest("同じ承認者を複数ステップに指定できません");
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
            String stepStatus = order == 1 ? "PENDING" : "WAITING";
            steps.save(ApprovalStep.of(request.getId(), order++, approverId, stepStatus));
        }
        histories.save(ApprovalHistory.of(request.getId(), actor.getId(), "SUBMIT", blankToNull(req.comment())));
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

    @Transactional
    public ApprovalDtos.RequestView cancel(Long requestId, ApprovalDtos.DecisionRequest req, AppUser actor) {
        ApprovalRequest request = require(requestId);
        if (!OPEN.contains(request.getStatus())) {
            throw ApiException.conflict("この申請は既に完了しています");
        }
        if (!request.getRequestedBy().equals(actor.getId()) && !"ADMIN".equals(actor.getRole())) {
            throw ApiException.forbidden("申請者または管理者だけが取下げできます");
        }
        request.setStatus("CANCELLED");
        closeRemainingSteps(steps.findByRequestIdOrderByStepOrderAsc(requestId), null, "CANCELLED");
        histories.save(ApprovalHistory.of(requestId, actor.getId(), "CANCEL", blankToNull(req == null ? null : req.comment())));
        requests.save(request);
        contracts.applyApprovalOutcome(contracts.require(request.getContractId()), "RETURNED");
        audit.record(actor.getId(), "APPROVAL", request.getId(), "CANCEL");
        return toView(request);
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
                    if (!OPEN.contains(request.getStatus())) {
                        return null;
                    }
                    Contract contract = contracts.require(request.getContractId());
                    return new ApprovalDtos.InboxItem(
                            request.getId(), request.getContractId(), contract.getContractNo(), contract.getName(),
                            request.getVersionId(), request.getStatus(), step.getStepOrder(),
                            displayName(request.getRequestedBy()));
                })
                .filter(item -> item != null)
                .toList();
    }

    private ApprovalDtos.RequestView decide(Long requestId, AppUser actor, String action, String comment) {
        ApprovalRequest request = require(requestId);
        if (!OPEN.contains(request.getStatus())) {
            throw ApiException.conflict("この申請は既に完了しています");
        }
        if (("RETURN".equals(action) || "REJECT".equals(action)) && (comment == null || comment.isBlank())) {
            throw ApiException.badRequest("差戻し・却下にはコメントが必要です");
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
            histories.save(ApprovalHistory.of(requestId, actor.getId(), "APPROVE", blankToNull(comment)));
            ApprovalStep next = nextOpenStep(stepList, current);
            if (next != null) {
                next.setStatus("PENDING");
                steps.save(next);
                request.setStatus("IN_PROGRESS");
            } else {
                request.setStatus("APPROVED");
                contracts.applyApprovalOutcome(contracts.require(request.getContractId()), "APPROVED");
            }
        } else if ("RETURN".equals(action)) {
            current.setStatus("RETURNED");
            steps.save(current);
            closeRemainingSteps(stepList, current, "CANCELLED");
            request.setStatus("RETURNED");
            histories.save(ApprovalHistory.of(requestId, actor.getId(), "RETURN", comment.trim()));
            contracts.applyApprovalOutcome(contracts.require(request.getContractId()), "RETURNED");
        } else if ("REJECT".equals(action)) {
            current.setStatus("REJECTED");
            steps.save(current);
            closeRemainingSteps(stepList, current, "CANCELLED");
            request.setStatus("REJECTED");
            histories.save(ApprovalHistory.of(requestId, actor.getId(), "REJECT", comment.trim()));
            contracts.applyApprovalOutcome(contracts.require(request.getContractId()), "REJECTED");
        } else {
            throw ApiException.badRequest("未知の承認操作です");
        }
        requests.save(request);
        audit.record(actor.getId(), "APPROVAL", request.getId(), action);
        return toView(request);
    }

    private ApprovalStep nextOpenStep(List<ApprovalStep> stepList, ApprovalStep current) {
        return stepList.stream()
                .filter(s -> !s.getId().equals(current.getId()))
                .filter(s -> "WAITING".equals(s.getStatus()) || "PENDING".equals(s.getStatus()))
                .min(java.util.Comparator.comparing(ApprovalStep::getStepOrder))
                .orElse(null);
    }

    private void closeRemainingSteps(List<ApprovalStep> stepList, ApprovalStep current, String status) {
        stepList.stream()
                .filter(s -> current == null || !s.getId().equals(current.getId()))
                .filter(s -> "WAITING".equals(s.getStatus()) || "PENDING".equals(s.getStatus()))
                .forEach(s -> {
                    s.setStatus(status);
                    steps.save(s);
                });
    }

    private ApprovalRequest require(Long id) {
        return requests.findById(id).orElseThrow(() -> ApiException.notFound("承認申請が見つかりません"));
    }

    private ApprovalDtos.RequestView toView(ApprovalRequest request) {
        Contract contract = contracts.require(request.getContractId());
        ContractVersion version = contracts.requireVersion(request.getVersionId());
        List<ApprovalDtos.StepView> stepViews = steps.findByRequestIdOrderByStepOrderAsc(request.getId()).stream()
                .map(s -> new ApprovalDtos.StepView(
                        s.getId(), s.getStepOrder(), s.getApproverId(), displayName(s.getApproverId()), s.getStatus()))
                .toList();
        List<ApprovalDtos.HistoryView> historyViews = histories.findByRequestIdOrderByActedAtAsc(request.getId()).stream()
                .map(h -> new ApprovalDtos.HistoryView(
                        h.getId(), h.getActorId(), displayName(h.getActorId()), h.getAction(), h.getCommentText(), h.getActedAt()))
                .toList();
        ApprovalDtos.StepView current = stepViews.stream()
                .filter(s -> "PENDING".equals(s.status()))
                .findFirst()
                .orElse(null);
        return new ApprovalDtos.RequestView(
                request.getId(), request.getContractId(), contract.getContractNo(), contract.getName(),
                request.getVersionId(), version.getVersionNo(), request.getStatus(),
                request.getRequestedBy(), displayName(request.getRequestedBy()), request.getRequestedAt(),
                current == null ? null : current.stepOrder(),
                current == null ? null : current.approverId(),
                stepViews, historyViews);
    }

    private String displayName(Long userId) {
        return users.findById(userId).map(AppUser::getDisplayName).orElse("不明");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
