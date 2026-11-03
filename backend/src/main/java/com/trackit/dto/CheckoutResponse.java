package com.trackit.dto;

public record CheckoutResponse(String urlPaiement, String referenceSession, boolean simulation) {
}
