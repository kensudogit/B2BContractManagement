package com.example.contracts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.contracts.approval.ApprovalDtos;
import com.example.contracts.approval.ApprovalService;
import com.example.contracts.common.ApiException;
import com.example.contracts.contract.ContractDtos;
import com.example.contracts.contract.ContractService;
import com.example.contracts.party.Party;
import com.example.contracts.party.PartyRepository;
import com.example.contracts.security.AppUser;
import com.example.contracts.security.AppUserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContractFlowTest {

    @Autowired ContractService contracts;
    @Autowired ApprovalService approvals;
    @Autowired AppUserRepository users;
    @Autowired PartyRepository parties;
    @Autowired PasswordEncoder encoder;

    private AppUser sales;
    private AppUser approver;
    private Long partyId;

    @BeforeEach
    void seed() {
        sales = users.save(AppUser.create("sales", encoder.encode("password"), "営業", "SALES"));
        approver = users.save(AppUser.create("approver", encoder.encode("password"), "承認者", "APPROVER"));
        partyId = parties.save(Party.create("株式会社テスト", "1111111111111")).getId();
    }

    @Test
    void createSearchAndRejectInvalidPeriod() {
        var created = contracts.create(new ContractDtos.CreateRequest(
                "C-001", "基本契約", "MASTER", null,
                LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31),
                new BigDecimal("1200000.00"), "JPY", true, 30,
                List.of(new ContractDtos.PartyRef(partyId, "CUSTOMER"))), sales);
        assertThat(created.contract().status()).isEqualTo("DRAFT");
        assertThat(created.versions()).isNotEmpty();

        var page = contracts.search("基本", "DRAFT", null, 0, 20, "createdAt", "desc");
        assertThat(page.getTotalElements()).isEqualTo(1);

        assertThatThrownBy(() -> contracts.create(new ContractDtos.CreateRequest(
                "C-002", "不正期間", "SPOT", null,
                LocalDate.of(2027, 1, 1), LocalDate.of(2026, 1, 1),
                BigDecimal.TEN, "JPY", false, 0, List.of()), sales))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("終了日");
    }

    @Test
    void approvalFreezesVersionAndBlocksEdit() {
        var created = contracts.create(new ContractDtos.CreateRequest(
                "C-010", "個別契約", "SPOT", null,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30),
                new BigDecimal("500000"), "JPY", false, null,
                List.of(new ContractDtos.PartyRef(partyId, "CUSTOMER"))), sales);

        var request = approvals.submit(created.contract().id(),
                new ApprovalDtos.SubmitRequest(List.of(approver.getId()), "確認お願いします"),
                "idem-1", sales);
        assertThat(request.status()).isEqualTo("REQUESTED");
        assertThat(contracts.get(created.contract().id()).contract().status()).isEqualTo("UNDER_REVIEW");

        var again = approvals.submit(created.contract().id(),
                new ApprovalDtos.SubmitRequest(List.of(approver.getId()), "再送"),
                "idem-1", sales);
        assertThat(again.id()).isEqualTo(request.id());

        assertThatThrownBy(() -> contracts.update(created.contract().id(), new ContractDtos.UpdateRequest(
                created.contract().rowVersion(), "書き換え", "SPOT", null,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30),
                new BigDecimal("500000"), "JPY", false, null, List.of()), sales))
                .isInstanceOf(ApiException.class);

        var approved = approvals.approve(request.id(), new ApprovalDtos.DecisionRequest("問題なし"), approver);
        assertThat(approved.status()).isEqualTo("APPROVED");
        assertThat(contracts.get(created.contract().id()).contract().status()).isEqualTo("APPROVED");

        var active = contracts.activate(created.contract().id(), sales);
        assertThat(active.contract().status()).isEqualTo("ACTIVE");
    }
}
