package com.example.contracts.document;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contract_documents")
public class ContractDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String sha256;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public Long getContractId() { return contractId; }
    public Long getVersionId() { return versionId; }
    public String getFileName() { return fileName; }
    public String getStorageKey() { return storageKey; }
    public String getSha256() { return sha256; }
    public String getContentType() { return contentType; }
    public OffsetDateTime getUploadedAt() { return uploadedAt; }

    public static ContractDocument of(Long contractId, Long versionId, String fileName,
            String storageKey, String sha256, String contentType) {
        ContractDocument d = new ContractDocument();
        d.contractId = contractId;
        d.versionId = versionId;
        d.fileName = fileName;
        d.storageKey = storageKey;
        d.sha256 = sha256;
        d.contentType = contentType;
        return d;
    }
}
