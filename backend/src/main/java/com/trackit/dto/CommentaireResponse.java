package com.trackit.dto;

import java.time.LocalDateTime;

public record CommentaireResponse(Long id, String contenu, LocalDateTime dateCreation, UtilisateurResume auteur) {
}
