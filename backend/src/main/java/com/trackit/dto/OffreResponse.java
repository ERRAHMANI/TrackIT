package com.trackit.dto;

import java.math.BigDecimal;

public record OffreResponse(String code, String libelle, BigDecimal montant, String devise, int dureeMois) {
}
