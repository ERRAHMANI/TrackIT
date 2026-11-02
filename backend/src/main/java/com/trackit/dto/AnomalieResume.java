package com.trackit.dto;

import java.time.LocalDateTime;

public record AnomalieResume(
        Long id,
        String titre,
        RefResponse systeme,
        RefResponse type,
        PrioriteResponse priorite,
        StatutResponse statut,
        UtilisateurResume auteur,
        UtilisateurResume operateur,
        LocalDateTime dateCreation,
        LocalDateTime dateResolution) {
}
