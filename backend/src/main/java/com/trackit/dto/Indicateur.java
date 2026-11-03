package com.trackit.dto;

/** Valeur d'un indicateur et sa variation (en %) par rapport à la période précédente de même durée. */
public record Indicateur(long valeur, Double variationPourcent) {
}
