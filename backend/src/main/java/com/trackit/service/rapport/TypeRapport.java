package com.trackit.service.rapport;

import java.util.Arrays;

public enum TypeRapport {
    SYNTHESE("Synthèse statistique"),
    LISTE_ANOMALIES("Liste détaillée des anomalies");

    private final String libelle;

    TypeRapport(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static TypeRapport fromLibelle(String libelle) {
        return Arrays.stream(values()).filter(t -> t.libelle.equals(libelle)).findFirst().orElse(SYNTHESE);
    }
}
