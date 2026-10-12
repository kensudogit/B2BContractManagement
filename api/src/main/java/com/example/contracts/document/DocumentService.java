package com.example.contracts.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.contracts.audit.AuditService;
import com.example.contracts.common.ApiException;
import com.example.contracts.contract.Contract;
import com.example.contracts.contract.ContractService;
import com.example.contracts.security.AppUser;

@Service
public class DocumentService {
    private static final Set<String> ALLOWED = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/png",
            "image/jpeg");

    private final ContractDocumentRepository documents;
    private final ContractService contracts;
    private final AuditService audit;
    private final Path root;

    public DocumentService(
            ContractDocumentRepository documents,
            ContractService contracts,
            AuditService audit,
            @Value("${app.document-dir}") String documentDir) {
        this.documents = documents;
        this.contracts = contracts;
        this.audit = audit;
        this.root = Path.of(documentDir).toAbsolutePath().normalize();
    }

    @Transactional
    public DocumentView upload(Long contractId, MultipartFile file, AppUser actor) {
        Contract contract = contracts.require(contractId);
        if (!Set.of("DRAFT", "APPROVED", "PENDING_SIGNATURE", "ACTIVE").contains(contract.getStatus())) {
            throw ApiException.conflict("この状態の契約には文書を追加できません");
        }
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("ファイルを選択してください");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(contentType)) {
            throw ApiException.badRequest("PDF / DOCX / PNG / JPEG のみアップロードできます");
        }
        String original = safeName(file.getOriginalFilename());
        String key = contractId + "/" + UUID.randomUUID() + "-" + original;
        Path dest = root.resolve(key).normalize();
        if (!dest.startsWith(root)) {
            throw ApiException.badRequest("不正なファイル名です");
        }
        try {
            Files.createDirectories(dest.getParent());
            byte[] bytes = file.getBytes();
            Files.write(dest, bytes);
            String sha = sha256(bytes);
            Long versionId = contracts.latestVersion(contractId).getId();
            ContractDocument saved = documents.save(
                    ContractDocument.of(contractId, versionId, original, key, sha, contentType));
            audit.record(actor.getId(), "DOCUMENT", saved.getId(), "UPLOAD");
            return DocumentView.of(saved);
        } catch (IOException e) {
            throw ApiException.badRequest("ファイルの保存に失敗しました");
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentView> list(Long contractId) {
        contracts.require(contractId);
        return documents.findByContractIdOrderByUploadedAtDesc(contractId).stream().map(DocumentView::of).toList();
    }

    @Transactional(readOnly = true)
    public StoredFile download(Long contractId, Long documentId) {
        contracts.require(contractId);
        ContractDocument doc = documents.findById(documentId)
                .orElseThrow(() -> ApiException.notFound("文書が見つかりません"));
        if (!doc.getContractId().equals(contractId)) {
            throw ApiException.notFound("文書が見つかりません");
        }
        Path path = root.resolve(doc.getStorageKey()).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw ApiException.notFound("ファイル実体が見つかりません");
        }
        return new StoredFile(new FileSystemResource(path), doc.getFileName(), doc.getContentType());
    }

    public record DocumentView(Long id, Long contractId, Long versionId, String fileName, String sha256,
            String contentType, java.time.OffsetDateTime uploadedAt) {
        static DocumentView of(ContractDocument d) {
            return new DocumentView(d.getId(), d.getContractId(), d.getVersionId(), d.getFileName(),
                    d.getSha256(), d.getContentType(), d.getUploadedAt());
        }
    }

    public record StoredFile(Resource resource, String fileName, String contentType) {
        public MediaType mediaType() {
            return MediaType.parseMediaType(contentType);
        }
    }

    private static String safeName(String name) {
        String base = name == null || name.isBlank() ? "document" : Path.of(name).getFileName().toString();
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("ハッシュ計算に失敗しました");
        }
    }
}
