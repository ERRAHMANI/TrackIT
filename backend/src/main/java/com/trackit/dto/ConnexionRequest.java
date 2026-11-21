package com.trackit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ConnexionRequest(
        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail n'est pas valide.")
        String email,
        @NotBlank(message = "Le mot de passe est obligatoire.")
        String motDePasse,
        /** « Se souvenir de moi » : cookie de session persistant. Facultatif (faux par défaut). */
        Boolean seSouvenir) {

    public boolean persistant() {
        return Boolean.TRUE.equals(seSouvenir);
    }
}
