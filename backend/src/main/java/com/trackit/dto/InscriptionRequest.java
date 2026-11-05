package com.trackit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Inscription d'une nouvelle organisation et de son premier administrateur. */
public record InscriptionRequest(
        @NotBlank(message = "Le nom de l'organisation est obligatoire.")
        @Size(max = 150, message = "Le nom de l'organisation ne doit pas dépasser 150 caractères.")
        String nomOrganisation,
        @NotBlank(message = "L'e-mail de l'organisation est obligatoire.")
        @Email(message = "L'e-mail de l'organisation n'est pas valide.")
        @Size(max = 150, message = "L'e-mail ne doit pas dépasser 150 caractères.")
        String emailOrganisation,
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
        String nom,
        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
        String prenom,
        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail n'est pas valide.")
        @Size(max = 150, message = "L'e-mail ne doit pas dépasser 150 caractères.")
        String email,
        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères.")
        String motDePasse) {
}
