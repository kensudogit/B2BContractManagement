package com.example.contracts.contract;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_versions")
public class ContractVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_json", nullable = false)
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public Long getContractId() { return contractId; }
    public Integer getVersionNo() { return versionNo; }
    public String getSnapshotJson() { return snapshotJson; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public static ContractVersion of(Long contractId, int versionNo, String snapshotJson) {
        ContractVersion v = new ContractVersion();
        v.contractId = contractId;
        v.versionNo = versionNo;
        v.snapshotJson = snapshotJson;
        return v;
    }
}
