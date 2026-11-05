package com.trackit.dto;

/** Chiffres clés de la page d'accueil publique, calculés depuis la base. */
public record ChiffresPublicsResponse(long organisations, long anomaliesTraitees, Double tauxResolutionPourcent,
                                      long systemesSuivis) {
}
