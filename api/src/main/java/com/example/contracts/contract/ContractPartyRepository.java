package com.example.contracts.contract;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractPartyRepository extends JpaRepository<ContractParty, Long> {
    List<ContractParty> findByContractId(Long contractId);
}
