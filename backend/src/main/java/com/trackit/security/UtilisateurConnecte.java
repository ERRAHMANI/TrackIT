package com.trackit.security;

import com.trackit.entity.RoleCode;
import com.trackit.entity.Utilisateur;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Principal Spring Security : identité, rôle et organisation de l'utilisateur authentifié. */
public record UtilisateurConnecte(
        Long id,
        String email,
        String motDePasse,
        String nomComplet,
        RoleCode role,
        Long organisationId,
        boolean actif) implements UserDetails {

    public static UtilisateurConnecte depuis(Utilisateur utilisateur) {
        return new UtilisateurConnecte(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getMotDePasse(),
                utilisateur.getNomComplet(),
                utilisateur.getRoleCode(),
                utilisateur.getOrganisation().getId(),
                utilisateur.isActif());
    }

    public boolean aLeRole(RoleCode... roles) {
        for (RoleCode r : roles) {
            if (role == r) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}
