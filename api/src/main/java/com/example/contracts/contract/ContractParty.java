package com.example.contracts.contract;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_parties")
public class ContractParty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_party_id")
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "party_role", nullable = false)
    private String partyRole;

    public Long getId() { return id; }
    public Long getContractId() { return contractId; }
    public Long getPartyId() { return partyId; }
    public String getPartyRole() { return partyRole; }

    public static ContractParty of(Long contractId, Long partyId, String role) {
        ContractParty cp = new ContractParty();
        cp.contractId = contractId;
        cp.partyId = partyId;
        cp.partyRole = role;
        return cp;
    }
}
