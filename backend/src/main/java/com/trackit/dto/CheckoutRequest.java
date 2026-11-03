package com.trackit.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(@NotBlank(message = "L'offre est obligatoire.") String codeOffre) {
}
