package com.trackit.dto;

import com.trackit.entity.FormatRapport;
import com.trackit.service.rapport.TypeRapport;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RapportRequest(
        @NotBlank(message = "Le titre du rapport est obligatoire.")
        @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères.")
        String titre,
        @NotNull(message = "Le type de rapport est obligatoire.")
        TypeRapport type,
        @NotNull(message = "Le format est obligatoire.")
        FormatRapport format,
        @NotNull(message = "La date de début est obligatoire.")
        LocalDate dateDebut,
        @NotNull(message = "La date de fin est obligatoire.")
        LocalDate dateFin) {

    @AssertTrue(message = "La date de fin doit être postérieure ou égale à la date de début.")
    public boolean isPeriodeValide() {
        return dateDebut == null || dateFin == null || !dateFin.isBefore(dateDebut);
    }
}
