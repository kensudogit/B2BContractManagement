package com.example.contracts.contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long id;

    @Column(name = "parent_contract_id")
    private Long parentContractId;

    @Column(name = "contract_no", nullable = false, unique = true, length = 50)
    private String contractNo;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "JPY";

    @Column(name = "auto_renewal", nullable = false)
    private boolean autoRenewal;

    @Column(name = "notice_days")
    private Integer noticeDays;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public Long getParentContractId() { return parentContractId; }
    public String getContractNo() { return contractNo; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrencyCode() { return currencyCode; }
    public boolean isAutoRenewal() { return autoRenewal; }
    public Integer getNoticeDays() { return noticeDays; }
    public Long getRowVersion() { return rowVersion; }

    public void setParentContractId(Long parentContractId) { this.parentContractId = parentContractId; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setStatus(String status) { this.status = status; this.updatedAt = OffsetDateTime.now(); }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public void setAutoRenewal(boolean autoRenewal) { this.autoRenewal = autoRenewal; }
    public void setNoticeDays(Integer noticeDays) { this.noticeDays = noticeDays; }
    public void setRowVersion(Long rowVersion) { this.rowVersion = rowVersion; }
}
