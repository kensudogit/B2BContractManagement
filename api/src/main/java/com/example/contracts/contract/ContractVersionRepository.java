package com.example.contracts.contract;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractVersionRepository extends JpaRepository<ContractVersion, Long> {
    List<ContractVersion> findByContractIdOrderByVersionNoDesc(Long contractId);

    Optional<ContractVersion> findTopByContractIdOrderByVersionNoDesc(Long contractId);
}
