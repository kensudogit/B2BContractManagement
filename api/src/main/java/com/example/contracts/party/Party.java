package com.example.contracts.party;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parties")
public class Party {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long id;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "corporate_number")
    private String corporateNumber;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public String getLegalName() { return legalName; }
    public String getCorporateNumber() { return corporateNumber; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public static Party create(String legalName, String corporateNumber) {
        Party p = new Party();
        p.legalName = legalName;
        p.corporateNumber = corporateNumber;
        return p;
    }
}
