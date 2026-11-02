package com.trackit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentaireRequest(
        @NotBlank(message = "Le commentaire ne peut pas être vide.")
        @Size(max = 5000, message = "Le commentaire ne doit pas dépasser 5 000 caractères.")
        String contenu) {
}
