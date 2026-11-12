package com.trackit.dto;

import com.trackit.validation.MotDePasseRobuste;
import jakarta.validation.constraints.NotBlank;

/** Réinitialisation d'un mot de passe par l'administrateur. */
public record MotDePasseRequest(
        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @MotDePasseRobuste
        String nouveauMotDePasse) {
}
