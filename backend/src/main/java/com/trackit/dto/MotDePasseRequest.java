package com.trackit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Réinitialisation d'un mot de passe par l'administrateur. */
public record MotDePasseRequest(
        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères.")
        String nouveauMotDePasse) {
}
