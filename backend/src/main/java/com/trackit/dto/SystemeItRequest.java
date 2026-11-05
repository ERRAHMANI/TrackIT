package com.trackit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SystemeItRequest(
        @NotBlank(message = "Le nom du système est obligatoire.")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères.")
        String nom,
        String description,
        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères.")
        String adresse,
        Boolean actif) {
}
