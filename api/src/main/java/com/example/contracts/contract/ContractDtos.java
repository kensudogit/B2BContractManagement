package com.example.contracts.contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ContractDtos {
    private ContractDtos() {}

    public record PartyRef(@NotNull Long partyId, @NotBlank String role) {}

    public record CreateRequest(
            @NotBlank @Size(max = 50) String contractNo,
            @NotBlank @Size(max = 200) String name,
            @NotBlank String type,
            Long parentContractId,
            LocalDate startDate,
            LocalDate endDate,
            @DecimalMin("0.00") BigDecimal amount,
            String currencyCode,
            boolean autoRenewal,
            Integer noticeDays,
            @Valid List<PartyRef> parties
    ) {}

    public record UpdateRequest(
            @NotNull Long rowVersion,
            @NotBlank @Size(max = 200) String name,
            @NotBlank String type,
            Long parentContractId,
            LocalDate startDate,
            LocalDate endDate,
            @DecimalMin("0.00") BigDecimal amount,
            String currencyCode,
            boolean autoRenewal,
            Integer noticeDays,
            @Valid List<PartyRef> parties
    ) {}

    public record RenewalRequest(@NotNull LocalDate newEndDate, String comment) {}

    public record TerminationRequest(String comment) {}

    public record PartyView(Long partyId, String legalName, String role) {}

    public record VersionView(Long id, Integer versionNo, OffsetDateTime createdAt, String snapshotJson) {}

    public record Summary(
            Long id,
            String contractNo,
            String name,
            String type,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal amount,
            String currencyCode,
            boolean autoRenewal,
            Integer noticeDays,
            Long parentContractId,
            Long rowVersion
    ) {
        public static Summary of(Contract c) {
            return new Summary(
                    c.getId(), c.getContractNo(), c.getName(), c.getType(), c.getStatus(),
                    c.getStartDate(), c.getEndDate(), c.getAmount(), c.getCurrencyCode(),
                    c.isAutoRenewal(), c.getNoticeDays(), c.getParentContractId(), c.getRowVersion());
        }
    }

    public record Detail(
            Summary contract,
            List<PartyView> parties,
            List<VersionView> versions
    ) {}
}
