package com.trackit.dto;

import java.time.LocalDateTime;

public record UtilisateurResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        boolean actif,
        LocalDateTime dateCreation,
        RefResponse role,
        String codeRole,
        RefResponse organisation) {
}
