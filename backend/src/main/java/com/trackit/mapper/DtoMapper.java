package com.trackit.mapper;

import com.trackit.dto.RefResponse;
import com.trackit.dto.UtilisateurResponse;
import com.trackit.dto.UtilisateurResume;
import com.trackit.entity.Utilisateur;

/** Conversions entité → DTO. Les méthodes doivent être appelées dans une transaction ouverte. */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static UtilisateurResponse utilisateur(Utilisateur u) {
        return new UtilisateurResponse(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.isActif(),
                u.getDateCreation(), new RefResponse(u.getRole().getId(), u.getRole().getNom()),
                u.getRoleCode().name(),
                new RefResponse(u.getOrganisation().getId(), u.getOrganisation().getNom()));
    }

    public static UtilisateurResume resume(Utilisateur u) {
        return u == null ? null : new UtilisateurResume(u.getId(), u.getNomComplet(), u.getRole().getNom());
    }
}