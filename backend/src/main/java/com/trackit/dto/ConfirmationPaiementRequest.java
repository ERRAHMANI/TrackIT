package com.trackit.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmationPaiementRequest(
        @NotBlank(message = "La référence de session est obligatoire.") String referenceSession) {
}
