package com.trackit.entity;

import java.util.Arrays;
import java.util.Set;

/**
 * Les quatre statuts officiels d'une anomalie et les transitions autorisées
 * (diagramme d'état-transition, sans les états Assignée et Suspendue).
 */
public enum StatutCode {
    NOUVELLE("Nouvelle"),
    EN_COURS("En cours"),
    RESOLUE("Résolue"),
    FERMEE("Fermée");

    private final String libelle;

    StatutCode(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public Set<StatutCode> transitionsAutorisees() {
        return switch (this) {
            case NOUVELLE -> Set.of(EN_COURS);
            case EN_COURS -> Set.of(RESOLUE);
            case RESOLUE -> Set.of(EN_COURS, FERMEE);
            case FERMEE -> Set.of();
        };
    }

    public boolean peutPasserA(StatutCode cible) {
        return transitionsAutorisees().contains(cible);
    }

    public boolean estOuvert() {
        return this == NOUVELLE || this == EN_COURS;
    }

    public static StatutCode fromLibelle(String libelle) {
        return Arrays.stream(values())
                .filter(code -> code.libelle.equalsIgnoreCase(libelle))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Statut inconnu : " + libelle));
    }
}
