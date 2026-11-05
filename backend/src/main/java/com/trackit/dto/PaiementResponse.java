package com.trackit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaiementResponse(Long id, BigDecimal montant, String devise, LocalDateTime datePaiement, String statut,
                               String referenceStripe, Long idAbonnement) {
}
