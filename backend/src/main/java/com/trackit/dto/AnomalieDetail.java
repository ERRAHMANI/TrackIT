package com.trackit.dto;

import java.time.LocalDateTime;

public record AnomalieDetail(
        Long id,
        String titre,
        String description,
        RefResponse systeme,
        RefResponse type,
        PrioriteResponse priorite,
        StatutResponse statut,
        UtilisateurResume auteur,
        UtilisateurResume operateur,
        String organisation,
        LocalDateTime dateCreation,
        LocalDateTime dateResolution,
        LocalDateTime derniereMiseAJour,
        long nombreCommentaires,
        long nombrePiecesJointes,
        long nombreEvenements,
        ActionsAutorisees actions) {
}
