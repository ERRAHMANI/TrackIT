package com.trackit.service.rapport;

import com.trackit.dto.AnomalieResume;
import com.trackit.dto.StatistiquesResponse;

import java.time.LocalDateTime;
import java.util.List;

/** Données d'un rapport, indépendantes du format de sortie. */
public record ContenuRapport(
        String titre,
        TypeRapport type,
        String organisation,
        String auteur,
        LocalDateTime dateGeneration,
        StatistiquesResponse statistiques,
        List<AnomalieResume> anomalies) {
}
