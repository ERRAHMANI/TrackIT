package com.trackit.dto;

import java.time.LocalDateTime;

public record HistoriqueResponse(
        Long id,
        String action,
        String ancienStatut,
        String nouveauStatut,
        String anciennePriorite,
        String nouvellePriorite,
        LocalDateTime dateModification,
        UtilisateurResume utilisateur) {
}
