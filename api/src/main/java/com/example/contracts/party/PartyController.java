package com.example.contracts.party;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/parties")
public class PartyController {
    private final PartyRepository parties;

    public PartyController(PartyRepository parties) {
        this.parties = parties;
    }

    public record PartyResponse(Long id, String legalName, String corporateNumber) {
        static PartyResponse of(Party p) {
            return new PartyResponse(p.getId(), p.getLegalName(), p.getCorporateNumber());
        }
    }

    public record PartyCreateRequest(@NotBlank String legalName, String corporateNumber) {}

    @GetMapping
    public List<PartyResponse> list() {
        return parties.findAll().stream().map(PartyResponse::of).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LEGAL','SALES')")
    public PartyResponse create(@Valid @RequestBody PartyCreateRequest body) {
        return PartyResponse.of(parties.save(Party.create(body.legalName(), body.corporateNumber())));
    }
}
