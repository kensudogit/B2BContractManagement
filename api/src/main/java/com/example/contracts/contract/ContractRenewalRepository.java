package com.example.contracts.contract;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRenewalRepository extends JpaRepository<ContractRenewal, Long> {
    List<ContractRenewal> findByContractIdOrderByDecidedAtDesc(Long contractId);
}
