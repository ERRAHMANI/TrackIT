package com.trackit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Déclaration ou modification d'une anomalie : les cinq champs obligatoires du formulaire. */
public record AnomalieRequest(
        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères.")
        String titre,
        @NotBlank(message = "La description est obligatoire.")
        @Size(max = 10000, message = "La description ne doit pas dépasser 10 000 caractères.")
        String description,
        @NotNull(message = "Le système concerné est obligatoire.")
        Long idSysteme,
        @NotNull(message = "Le type d'anomalie est obligatoire.")
        Long idType,
        @NotNull(message = "La priorité est obligatoire.")
        Long idPriorite) {
}
