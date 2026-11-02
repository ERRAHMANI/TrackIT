package com.trackit.dto;

import java.time.LocalDate;

/** Critères de recherche des anomalies ; tous facultatifs et cumulables. */
public record AnomalieCritere(
        String q,
        Long idStatut,
        Long idPriorite,
        Long idType,
        Long idSysteme,
        Long idOperateur,
        Boolean nonAffectee,
        Boolean mesDeclarations,
        Boolean affecteesAMoi,
        LocalDate dateDebut,
        LocalDate dateFin) {
}
