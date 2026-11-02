package com.trackit.dto;

import java.util.List;

/** Actions que l'utilisateur courant peut effectuer sur une anomalie (calculées côté serveur). */
public record ActionsAutorisees(
        boolean modifier,
        boolean changerStatut,
        List<StatutResponse> statutsPossibles,
        boolean prioriser,
        boolean affecter,
        boolean prendreEnCharge,
        boolean commenter,
        boolean joindreFichier,
        boolean supprimer) {
}
