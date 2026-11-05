package com.trackit.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> contenu, int page, int taille, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> depuis(Page<E> page, Function<E, T> conversion) {
        return new PageResponse<>(page.getContent().stream().map(conversion).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
