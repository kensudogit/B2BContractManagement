package com.example.contracts.contract;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_renewals")
public class ContractRenewal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "renewal_id")
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "new_end_date", nullable = false)
    private LocalDate newEndDate;

    @Column(nullable = false)
    private String decision;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public Long getContractId() { return contractId; }
    public LocalDate getNewEndDate() { return newEndDate; }
    public String getDecision() { return decision; }
    public OffsetDateTime getDecidedAt() { return decidedAt; }

    public static ContractRenewal of(Long contractId, LocalDate newEndDate, String decision) {
        ContractRenewal r = new ContractRenewal();
        r.contractId = contractId;
        r.newEndDate = newEndDate;
        r.decision = decision;
        return r;
    }
}
