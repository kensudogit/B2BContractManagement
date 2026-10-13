package com.example.contracts.approval;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.contracts.security.CurrentUser;

@RestController
@RequestMapping("/api/approval-requests")
public class ApprovalController {
    private final ApprovalService approvals;

    public ApprovalController(ApprovalService approvals) {
        this.approvals = approvals;
    }

    @GetMapping("/inbox")
    public List<ApprovalDtos.InboxItem> inbox() {
        return approvals.inbox(CurrentUser.require());
    }

    @GetMapping("/{id}")
    public ApprovalDtos.RequestView get(@PathVariable Long id) {
        return approvals.get(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER','LEGAL')")
    public ApprovalDtos.RequestView approve(@PathVariable Long id, @RequestBody(required = false) ApprovalDtos.DecisionRequest body) {
        return approvals.approve(id, body, CurrentUser.require());
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER','LEGAL')")
    public ApprovalDtos.RequestView returnToDraft(@PathVariable Long id, @RequestBody(required = false) ApprovalDtos.DecisionRequest body) {
        return approvals.returnToDraft(id, body, CurrentUser.require());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER','LEGAL')")
    public ApprovalDtos.RequestView reject(@PathVariable Long id, @RequestBody(required = false) ApprovalDtos.DecisionRequest body) {
        return approvals.reject(id, body, CurrentUser.require());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL','SALES')")
    public ApprovalDtos.RequestView cancel(@PathVariable Long id, @RequestBody(required = false) ApprovalDtos.DecisionRequest body) {
        return approvals.cancel(id, body, CurrentUser.require());
    }
}
