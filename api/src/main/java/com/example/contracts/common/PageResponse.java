package com.example.contracts.common;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(), page.getNumber());
    }
}
