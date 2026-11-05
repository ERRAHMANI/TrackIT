package com.trackit.dto;

public record ChargeOperateur(Long idOperateur, String nomComplet, long nouvelles, long enCours) {

    public long getTotal() {
        return nouvelles + enCours;
    }
}
