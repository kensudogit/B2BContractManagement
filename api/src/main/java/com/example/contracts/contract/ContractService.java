package com.example.contracts.contract;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.contracts.audit.AuditService;
import com.example.contracts.common.ApiException;
import com.example.contracts.party.Party;
import com.example.contracts.party.PartyRepository;
import com.example.contracts.security.AppUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ContractService {
    private static final Set<String> SORTS = Set.of("createdAt", "endDate", "contractNo", "name", "status");
    private static final Set<String> EDITABLE = Set.of("DRAFT");

    private final ContractRepository contracts;
    private final ContractPartyRepository contractParties;
    private final ContractVersionRepository versions;
    private final ContractRenewalRepository renewals;
    private final PartyRepository parties;
    private final AuditService audit;
    private final ObjectMapper mapper;

    public ContractService(
            ContractRepository contracts,
            ContractPartyRepository contractParties,
            ContractVersionRepository versions,
            ContractRenewalRepository renewals,
            PartyRepository parties,
            AuditService audit,
            ObjectMapper mapper) {
        this.contracts = contracts;
        this.contractParties = contractParties;
        this.versions = versions;
        this.renewals = renewals;
        this.parties = parties;
        this.audit = audit;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<ContractDtos.Summary> search(String q, String status, String type, int page, int size, String sort, String dir) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        String sortKey = SORTS.contains(sort) ? sort : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(direction, sortKey));
        String qPattern = blankToNull(q) == null ? "%" : "%" + q.trim().toLowerCase() + "%";
        return contracts.search(qPattern, blankToNull(status), blankToNull(type), pageable)
                .map(ContractDtos.Summary::of);
    }

    @Transactional(readOnly = true)
    public ContractDtos.Detail get(Long id) {
        Contract c = require(id);
        return toDetail(c);
    }

    @Transactional
    public ContractDtos.Detail create(ContractDtos.CreateRequest req, AppUser actor) {
        validatePeriod(req.startDate(), req.endDate());
        if (req.noticeDays() != null && req.noticeDays() < 0) {
            throw ApiException.badRequest("解約予告日数は0以上にしてください");
        }
        contracts.findByContractNo(req.contractNo()).ifPresent(existing -> {
            throw ApiException.conflict("契約番号が既に使われています");
        });
        if (req.parentContractId() != null) {
            require(req.parentContractId());
        }
        Contract c = new Contract();
        c.setContractNo(req.contractNo().trim());
        applyFields(c, req.name(), req.type(), req.parentContractId(), req.startDate(), req.endDate(),
                req.amount(), req.currencyCode(), req.autoRenewal(), req.noticeDays());
        c.setStatus("DRAFT");
        c = contracts.save(c);
        replaceParties(c.getId(), req.parties());
        freezeVersion(c);
        audit.record(actor.getId(), "CONTRACT", c.getId(), "CREATE");
        return toDetail(c);
    }

    @Transactional
    public ContractDtos.Detail update(Long id, ContractDtos.UpdateRequest req, AppUser actor) {
        Contract c = require(id);
        if (!EDITABLE.contains(c.getStatus())) {
            throw ApiException.conflict("下書き以外の契約は編集できません。差戻し後に再編集してください。");
        }
        if (c.getRowVersion() == null || !c.getRowVersion().equals(req.rowVersion())) {
            throw ApiException.conflict("他のユーザーが更新しています。再取得してからやり直してください。");
        }
        validatePeriod(req.startDate(), req.endDate());
        if (req.parentContractId() != null && !req.parentContractId().equals(c.getId())) {
            require(req.parentContractId());
        }
        applyFields(c, req.name(), req.type(), req.parentContractId(), req.startDate(), req.endDate(),
                req.amount(), req.currencyCode(), req.autoRenewal(), req.noticeDays());
        c = contracts.save(c);
        replaceParties(c.getId(), req.parties());
        freezeVersion(c);
        audit.record(actor.getId(), "CONTRACT", c.getId(), "UPDATE");
        return toDetail(c);
    }

    @Transactional
    public ContractDtos.Detail activate(Long id, AppUser actor) {
        Contract c = require(id);
        if (!Set.of("APPROVED", "PENDING_SIGNATURE").contains(c.getStatus())) {
            throw ApiException.conflict("承認済みまたは締結待ちの契約のみ有効化できます");
        }
        c.setStatus("ACTIVE");
        contracts.save(c);
        audit.record(actor.getId(), "CONTRACT", c.getId(), "ACTIVATE");
        return toDetail(c);
    }

    @Transactional
    public ContractDtos.Detail markSigned(Long id, AppUser actor) {
        Contract c = require(id);
        if (!"APPROVED".equals(c.getStatus())) {
            throw ApiException.conflict("承認済みの契約のみ締結待ちにできます");
        }
        c.setStatus("PENDING_SIGNATURE");
        contracts.save(c);
        audit.record(actor.getId(), "CONTRACT", c.getId(), "MARK_SIGNED");
        return toDetail(c);
    }

    @Transactional
    public ContractDtos.Detail renew(Long id, ContractDtos.RenewalRequest req, AppUser actor) {
        Contract c = require(id);
        if (!"ACTIVE".equals(c.getStatus())) {
            throw ApiException.conflict("有効な契約のみ期間更新できます");
        }
        if (c.getEndDate() != null && !req.newEndDate().isAfter(c.getEndDate())) {
            throw ApiException.badRequest("新しい終了日は現行終了日より後にしてください");
        }
        validatePeriod(c.getStartDate(), req.newEndDate());
        c.setEndDate(req.newEndDate());
        contracts.save(c);
        renewals.save(ContractRenewal.of(c.getId(), req.newEndDate(), "RENEWED"));
        freezeVersion(c);
        audit.record(actor.getId(), "CONTRACT", c.getId(), "RENEW");
        return toDetail(c);
    }

    @Transactional
    public ContractDtos.Detail terminate(Long id, ContractDtos.TerminationRequest req, AppUser actor) {
        Contract c = require(id);
        if (!"ACTIVE".equals(c.getStatus())) {
            throw ApiException.conflict("有効な契約のみ解約できます。満了とは区別してください。");
        }
        c.setStatus("TERMINATED");
        contracts.save(c);
        audit.record(actor.getId(), "CONTRACT", c.getId(), "TERMINATE");
        return toDetail(c);
    }

    @Transactional(readOnly = true)
    public List<ContractDtos.Summary> expiring(int days) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(Math.min(Math.max(days, 1), 365));
        return contracts.findByEndDateBetweenAndStatus(from, to, "ACTIVE").stream()
                .map(ContractDtos.Summary::of)
                .toList();
    }

    @Transactional
    public int expireOverdue() {
        List<Contract> due = contracts.findByStatusAndEndDateBefore("ACTIVE", LocalDate.now());
        due.forEach(c -> c.setStatus("EXPIRED"));
        contracts.saveAll(due);
        return due.size();
    }

    public Contract require(Long id) {
        return contracts.findById(id).orElseThrow(() -> ApiException.notFound("契約が見つかりません"));
    }

    public void requireEditable(Contract c) {
        if (!EDITABLE.contains(c.getStatus())) {
            throw ApiException.conflict("下書き以外は内容を差し替えできません");
        }
    }

    public ContractVersion latestVersion(Long contractId) {
        return versions.findTopByContractIdOrderByVersionNoDesc(contractId)
                .orElseThrow(() -> ApiException.badRequest("確定版がありません。先に契約を保存してください"));
    }

    public void markUnderReview(Contract c) {
        c.setStatus("UNDER_REVIEW");
        contracts.save(c);
    }

    public void applyApprovalOutcome(Contract c, String approvalStatus) {
        switch (approvalStatus) {
            case "APPROVED" -> c.setStatus("APPROVED");
            case "RETURNED" -> c.setStatus("DRAFT");
            case "REJECTED" -> c.setStatus("REJECTED");
            default -> throw ApiException.badRequest("未知の承認結果です");
        }
        contracts.save(c);
    }

    private void applyFields(Contract c, String name, String type, Long parentId, LocalDate start, LocalDate end,
            java.math.BigDecimal amount, String currency, boolean autoRenewal, Integer noticeDays) {
        c.setName(name.trim());
        c.setType(type.trim());
        c.setParentContractId(parentId);
        c.setStartDate(start);
        c.setEndDate(end);
        c.setAmount(amount);
        c.setCurrencyCode(currency == null || currency.isBlank() ? "JPY" : currency.trim());
        c.setAutoRenewal(autoRenewal);
        c.setNoticeDays(noticeDays);
    }

    private void replaceParties(Long contractId, List<ContractDtos.PartyRef> refs) {
        contractParties.deleteAll(contractParties.findByContractId(contractId));
        contractParties.flush();
        if (refs == null) {
            return;
        }
        for (ContractDtos.PartyRef ref : refs) {
            parties.findById(ref.partyId()).orElseThrow(() -> ApiException.badRequest("取引先が存在しません"));
            contractParties.save(ContractParty.of(contractId, ref.partyId(), ref.role().trim()));
        }
    }

    private void freezeVersion(Contract c) {
        int next = versions.findTopByContractIdOrderByVersionNoDesc(c.getId())
                .map(v -> v.getVersionNo() + 1)
                .orElse(1);
        versions.save(ContractVersion.of(c.getId(), next, snapshot(c)));
    }

    private String snapshot(Contract c) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("contractId", c.getId());
        snap.put("contractNo", c.getContractNo());
        snap.put("name", c.getName());
        snap.put("type", c.getType());
        snap.put("status", c.getStatus());
        snap.put("startDate", c.getStartDate());
        snap.put("endDate", c.getEndDate());
        snap.put("amount", c.getAmount());
        snap.put("currencyCode", c.getCurrencyCode());
        snap.put("autoRenewal", c.isAutoRenewal());
        snap.put("noticeDays", c.getNoticeDays());
        snap.put("parentContractId", c.getParentContractId());
        snap.put("parties", contractParties.findByContractId(c.getId()).stream()
                .map(p -> Map.of("partyId", p.getPartyId(), "role", p.getPartyRole()))
                .toList());
        try {
            return mapper.writeValueAsString(snap);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("契約スナップショットの作成に失敗しました");
        }
    }

    private ContractDtos.Detail toDetail(Contract c) {
        List<ContractDtos.PartyView> partyViews = contractParties.findByContractId(c.getId()).stream()
                .map(cp -> {
                    Party p = parties.findById(cp.getPartyId()).orElse(null);
                    return new ContractDtos.PartyView(
                            cp.getPartyId(),
                            p == null ? null : p.getLegalName(),
                            cp.getPartyRole());
                })
                .toList();
        List<ContractDtos.VersionView> versionViews = versions.findByContractIdOrderByVersionNoDesc(c.getId()).stream()
                .map(v -> new ContractDtos.VersionView(v.getId(), v.getVersionNo(), v.getCreatedAt(), v.getSnapshotJson()))
                .toList();
        return new ContractDtos.Detail(ContractDtos.Summary.of(c), partyViews, versionViews);
    }

    private static void validatePeriod(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw ApiException.badRequest("終了日は開始日以降にしてください");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
