package com.trackit.dto;

import com.trackit.validation.MotDePasseRobuste;
import jakarta.validation.constraints.NotBlank;

public record ChangementMotDePasseRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire.")
        String motDePasseActuel,
        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @MotDePasseRobuste
        String nouveauMotDePasse) {
}
