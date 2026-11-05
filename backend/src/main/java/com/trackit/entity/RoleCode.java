package com.trackit.entity;

import java.util.Arrays;

/**
 * Rôles définis par le diagramme de cas d'utilisation. Le libellé correspond à la
 * colonne role.nom ; le code sert d'autorité Spring Security (ROLE_xxx).
 */
public enum RoleCode {
    UTILISATEUR("Utilisateur"),
    OPERATEUR("Opérateur"),
    RESPONSABLE_TECHNIQUE("Responsable technique"),
    ANALYSTE("Analyste"),
    ADMINISTRATEUR("Administrateur");

    private final String libelle;

    RoleCode(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static RoleCode fromLibelle(String libelle) {
        return Arrays.stream(values())
                .filter(code -> code.libelle.equalsIgnoreCase(libelle))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Rôle inconnu : " + libelle));
    }
}
