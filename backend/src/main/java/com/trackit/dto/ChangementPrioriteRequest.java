package com.trackit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangementPrioriteRequest(
        @NotNull(message = "La priorité est obligatoire.")
        Long idPriorite,
        @Size(max = 2000, message = "Le commentaire ne doit pas dépasser 2 000 caractères.")
        String commentaire) {
}
