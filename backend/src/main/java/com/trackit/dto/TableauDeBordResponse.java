package com.trackit.dto;

import java.time.LocalDate;
import java.util.List;

public record TableauDeBordResponse(
        LocalDate debut,
        LocalDate fin,
        Indicateur total,
        Indicateur nouvelles,
        Indicateur enCours,
        Indicateur resolues,
        Indicateur fermees,
        List<PointEvolution> evolution,
        List<LibelleValeur> repartitionParType,
        List<AnomalieResume> anomaliesRecentes) {
}
