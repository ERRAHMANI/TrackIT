package com.trackit.dto;

import java.util.List;

public record HistoriqueAbonnementResponse(List<AbonnementResponse> abonnements, List<PaiementResponse> paiements) {
}
