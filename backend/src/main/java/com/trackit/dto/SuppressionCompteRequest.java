package com.trackit.dto;

import jakarta.validation.constraints.NotBlank;

/** Suppression de son propre compte : le mot de passe actuel confirme la demande. */
public record SuppressionCompteRequest(
        @NotBlank(message = "Le mot de passe est obligatoire.")
        String motDePasse) {
}
