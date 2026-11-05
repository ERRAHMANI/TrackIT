package com.trackit.dto;

import jakarta.validation.constraints.Size;

/** Seule la description d'un statut est modifiable : les quatre statuts sont fixes. */
public record StatutRequest(
        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères.")
        String description) {
}
