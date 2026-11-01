package com.trackit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrioriteRequest(
        @NotBlank(message = "Le nom de la priorité est obligatoire.")
        @Size(max = 50, message = "Le nom ne doit pas dépasser 50 caractères.")
        String nom,
        @NotNull(message = "Le niveau est obligatoire.")
        @Min(value = 1, message = "Le niveau doit être compris entre 1 et 4.")
        @Max(value = 4, message = "Le niveau doit être compris entre 1 et 4.")
        Integer niveau,
        @Size(max = 255, message = "La description ne doit pas dépasser 255 caractères.")
        String description) {
}
