package com.trackit.dto;

import java.time.LocalDate;

/**
 * Évolution journalière : anomalies déclarées le jour, anomalies non résolues en fin de journée
 * et anomalies résolues le jour.
 */
public record PointEvolution(LocalDate jour, long declarees, long nonResolues, long resolues) {
}
