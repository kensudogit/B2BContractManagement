package com.example.contracts.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractDocumentRepository extends JpaRepository<ContractDocument, Long> {
    List<ContractDocument> findByContractIdOrderByUploadedAtDesc(Long contractId);
}
