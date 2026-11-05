package com.trackit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErreurResponse(
        LocalDateTime horodatage,
        int status,
        String erreur,
        String message,
        String chemin,
        Map<String, String> champs) {
}
