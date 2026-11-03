package com.trackit.dto;

public record EtatPremiumResponse(boolean premiumActif, AbonnementResponse abonnementActif, boolean modeSimulation) {
}
