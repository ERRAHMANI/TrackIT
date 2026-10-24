package com.trackit.service;

import com.trackit.dto.*;
import com.trackit.entity.Role;
import com.trackit.entity.RoleCode;
import com.trackit.entity.Utilisateur;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.NotificationRepository;
import com.trackit.repository.RoleRepository;
import com.trackit.repository.UtilisateurRepository;
import com.trackit.security.UtilisateurConnecte;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Gestion des comptes (UC-04) et du profil de l'utilisateur connecté. */
@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, RoleRepository roleRepository,
                              NotificationRepository notificationRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------------------------------------------------ Administration

    @Transactional(readOnly = true)
    public PageResponse<UtilisateurResponse> lister(UtilisateurConnecte courant, String q, Long idRole, Boolean actif,
                                                    Pageable pageable) {
        Specification<Utilisateur> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("organisation").get("id"), courant.organisationId()));
            if (q != null && !q.isBlank()) {
                String motif = "%" + q.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("nom")), motif), cb.like(cb.lower(root.get("prenom")), motif),
                        cb.like(cb.lower(root.get("email")), motif)));
            }
            if (idRole != null) {
                p.add(cb.equal(root.get("role").get("id"), idRole));
            }
            if (actif != null) {
                p.add(cb.equal(root.get("actif"), actif));
            }
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("role");
                root.fetch("organisation");
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.depuis(utilisateurRepository.findAll(spec, pageable), DtoMapper::utilisateur);
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse obtenir(UtilisateurConnecte courant, Long id) {
        return DtoMapper.utilisateur(charger(courant, id));
    }

    @Transactional
    public UtilisateurResponse creer(UtilisateurConnecte courant, UtilisateurCreationRequest requete) {
        String email = requete.email().trim().toLowerCase();
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflit("Un compte existe déjà avec l'adresse « " + email + " ».");
        }
        Utilisateur administrateur = charger(courant, courant.id());
        Utilisateur u = new Utilisateur();
        u.setNom(requete.nom().trim().toUpperCase());
        u.setPrenom(requete.prenom().trim());
        u.setEmail(email);
        u.setMotDePasse(passwordEncoder.encode(requete.motDePasse()));
        u.setActif(requete.actif() == null || requete.actif());
        u.setRole(role(requete.idRole()));
        u.setOrganisation(administrateur.getOrganisation());
        utilisateurRepository.save(u);
        return DtoMapper.utilisateur(u);
    }

    @Transactional
    public UtilisateurResponse modifier(UtilisateurConnecte courant, Long id, UtilisateurModificationRequest requete) {
        Utilisateur u = charger(courant, id);
        String email = requete.email().trim().toLowerCase();
        if (utilisateurRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflit("Un compte existe déjà avec l'adresse « " + email + " ».");
        }
        Role role = role(requete.idRole());
        if (id.equals(courant.id()) && (!requete.actif() || role.getCode() != RoleCode.ADMINISTRATEUR)) {
            throw ApiException.conflit("Vous ne pouvez ni désactiver votre propre compte ni retirer votre rôle d'administrateur.");
        }
        u.setNom(requete.nom().trim().toUpperCase());
        u.setPrenom(requete.prenom().trim());
        u.setEmail(email);
        u.setRole(role);
        u.setActif(requete.actif());
        return DtoMapper.utilisateur(u);
    }

    @Transactional
    public void reinitialiserMotDePasse(UtilisateurConnecte courant, Long id, MotDePasseRequest requete) {
        Utilisateur u = charger(courant, id);
        u.setMotDePasse(passwordEncoder.encode(requete.nouveauMotDePasse()));
    }

    @Transactional
    public void supprimer(UtilisateurConnecte courant, Long id) {
        if (id.equals(courant.id())) {
            throw ApiException.conflit("Vous ne pouvez pas supprimer votre propre compte.");
        }
        Utilisateur u = charger(courant, id);
        try {
            notificationRepository.deleteByUtilisateurId(id);
            utilisateurRepository.delete(u);
            utilisateurRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflit("Ce compte est lié à des anomalies, commentaires ou rapports : "
                    + "désactivez-le plutôt que de le supprimer afin de conserver la traçabilité.");
        }
    }

    @Transactional(readOnly = true)
    public List<UtilisateurResume> operateursActifs(UtilisateurConnecte courant) {
        return utilisateurRepository.findByOrganisationIdAndRoleNomAndActifTrueOrderByNomAscPrenomAsc(
                        courant.organisationId(), RoleCode.OPERATEUR.getLibelle())
                .stream().map(DtoMapper::resume).toList();
    }

    // ------------------------------------------------------------------ Profil

    @Transactional(readOnly = true)
    public UtilisateurResponse profil(UtilisateurConnecte courant) {
        return DtoMapper.utilisateur(charger(courant, courant.id()));
    }

    @Transactional
    public UtilisateurResponse modifierProfil(UtilisateurConnecte courant, ProfilRequest requete) {
        Utilisateur u = charger(courant, courant.id());
        String email = requete.email().trim().toLowerCase();
        if (utilisateurRepository.existsByEmailIgnoreCaseAndIdNot(email, u.getId())) {
            throw ApiException.conflit("Un compte existe déjà avec l'adresse « " + email + " ».");
        }
        u.setNom(requete.nom().trim().toUpperCase());
        u.setPrenom(requete.prenom().trim());
        u.setEmail(email);
        return DtoMapper.utilisateur(u);
    }

    @Transactional
    public void changerMotDePasse(UtilisateurConnecte courant, ChangementMotDePasseRequest requete) {
        Utilisateur u = charger(courant, courant.id());
        if (!passwordEncoder.matches(requete.motDePasseActuel(), u.getMotDePasse())) {
            throw ApiException.invalide("Le mot de passe actuel est incorrect.");
        }
        if (requete.motDePasseActuel().equals(requete.nouveauMotDePasse())) {
            throw ApiException.invalide("Le nouveau mot de passe doit être différent de l'actuel.");
        }
        u.setMotDePasse(passwordEncoder.encode(requete.nouveauMotDePasse()));
    }

    private Utilisateur charger(UtilisateurConnecte courant, Long id) {
        return utilisateurRepository.findByIdAndOrganisationId(id, courant.organisationId())
                .orElseThrow(() -> ApiException.introuvable("Utilisateur introuvable."));
    }

    private Role role(Long idRole) {
        return roleRepository.findById(idRole).orElseThrow(() -> ApiException.invalide("Rôle inconnu."));
    }
}
