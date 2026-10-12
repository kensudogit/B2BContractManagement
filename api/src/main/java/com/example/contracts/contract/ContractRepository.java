package com.example.contracts.contract;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByContractNo(String contractNo);

    @Query("""
            SELECT c FROM Contract c
            WHERE (LOWER(c.name) LIKE :qPattern OR LOWER(c.contractNo) LIKE :qPattern)
              AND (:status IS NULL OR c.status = :status)
              AND (:type IS NULL OR c.type = :type)
            """)
    Page<Contract> search(@Param("qPattern") String qPattern, @Param("status") String status, @Param("type") String type, Pageable pageable);

    List<Contract> findByEndDateBetweenAndStatus(LocalDate from, LocalDate to, String status);

    List<Contract> findByStatusAndEndDateBefore(String status, LocalDate before);
}
