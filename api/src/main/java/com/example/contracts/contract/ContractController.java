package com.example.contracts.contract;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.contracts.approval.ApprovalDtos;
import com.example.contracts.approval.ApprovalService;
import com.example.contracts.document.DocumentService;
import com.example.contracts.security.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService contracts;
    private final ApprovalService approvals;
    private final DocumentService documents;

    public ContractController(ContractService contracts, ApprovalService approvals, DocumentService documents) {
        this.contracts = contracts;
        this.approvals = approvals;
        this.documents = documents;
    }

    @GetMapping({"", "/page"})
    public Page<ContractDtos.Summary> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return contracts.search(q, status, type, page, size, sort, dir);
    }

    @GetMapping("/expiring")
    public List<ContractDtos.Summary> expiring(@RequestParam(defaultValue = "90") int days) {
        return contracts.expiring(days);
    }

    @GetMapping("/{id}")
    public ContractDtos.Detail get(@PathVariable Long id) {
        return contracts.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL','SALES')")
    public ContractDtos.Detail create(@Valid @RequestBody ContractDtos.CreateRequest body) {
        return contracts.create(body, CurrentUser.require());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL','SALES')")
    public ContractDtos.Detail update(@PathVariable Long id, @Valid @RequestBody ContractDtos.UpdateRequest body) {
        return contracts.update(id, body, CurrentUser.require());
    }

    @PostMapping("/{id}/approval-requests")
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL','SALES')")
    public ApprovalDtos.RequestView submitApproval(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalDtos.SubmitRequest body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return approvals.submit(id, body, idempotencyKey, CurrentUser.require());
    }

    @GetMapping("/{id}/approval-requests")
    public List<ApprovalDtos.RequestView> approvals(@PathVariable Long id) {
        return approvals.byContract(id);
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL','SALES')")
    public DocumentService.DocumentView upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return documents.upload(id, file, CurrentUser.require());
    }

    @GetMapping("/{id}/documents")
    public List<DocumentService.DocumentView> documents(@PathVariable Long id) {
        return documents.list(id);
    }

    @GetMapping("/{id}/documents/{documentId}")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long id, @PathVariable Long documentId) {
        DocumentService.StoredFile file = documents.download(id, documentId);
        return ResponseEntity.ok()
                .contentType(file.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .body(file.resource());
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL')")
    public ContractDtos.Detail activate(@PathVariable Long id) {
        return contracts.activate(id, CurrentUser.require());
    }

    @PostMapping("/{id}/sign")
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL')")
    public ContractDtos.Detail sign(@PathVariable Long id) {
        return contracts.markSigned(id, CurrentUser.require());
    }

    @PostMapping("/{id}/renewals")
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL','SALES')")
    public ContractDtos.Detail renew(@PathVariable Long id, @Valid @RequestBody ContractDtos.RenewalRequest body) {
        return contracts.renew(id, body, CurrentUser.require());
    }

    @PostMapping("/{id}/termination")
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL')")
    public ContractDtos.Detail terminate(@PathVariable Long id, @RequestBody(required = false) ContractDtos.TerminationRequest body) {
        return contracts.terminate(id, body == null ? new ContractDtos.TerminationRequest(null) : body, CurrentUser.require());
    }
}
