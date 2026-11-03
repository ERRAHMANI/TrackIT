package com.trackit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SimulationPaiementRequest(
        @NotBlank(message = "La référence de session est obligatoire.") String referenceSession,
        @NotNull(message = "Le résultat simulé est obligatoire.") Resultat resultat) {

    public enum Resultat { SUCCES, ECHEC, ANNULATION }
}
