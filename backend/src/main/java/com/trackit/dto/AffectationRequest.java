package com.trackit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AffectationRequest(
        @NotNull(message = "L'opérateur est obligatoire.")
        Long idOperateur,
        @Size(max = 2000, message = "Le commentaire ne doit pas dépasser 2 000 caractères.")
        String commentaire,
        /** Version de la fiche affichée (facultative) : si elle n'est plus la dernière, 409 au lieu d'écraser. */
        Long version) {
}
