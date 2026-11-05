package com.trackit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangementStatutRequest(
        @NotNull(message = "Le nouveau statut est obligatoire.")
        Long idStatut,
        @Size(max = 2000, message = "Le commentaire ne doit pas dépasser 2 000 caractères.")
        String commentaire) {
}
