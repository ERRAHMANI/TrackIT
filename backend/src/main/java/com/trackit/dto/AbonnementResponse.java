package com.trackit.dto;

import java.time.LocalDate;

public record AbonnementResponse(Long id, String offre, LocalDate dateDebut, LocalDate dateFin, String statut) {
}
