package com.trackit.dto;

import java.time.LocalDate;
import java.util.List;

public record StatistiquesResponse(
        LocalDate debut,
        LocalDate fin,
        long totalDeclarees,
        long totalResolues,
        Double tauxResolutionPourcent,
        Double dureeMoyenneResolutionHeures,
        List<LibelleValeur> parStatut,
        List<LibelleValeur> parPriorite,
        List<LibelleValeur> parType,
        List<LibelleValeur> parSysteme,
        List<DureeMoyenne> dureeMoyenneParPriorite,
        List<IncidentRecurrent> incidentsRecurrents) {
}
