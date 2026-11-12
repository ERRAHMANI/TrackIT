package com.trackit.service;

import com.trackit.dto.*;
import com.trackit.entity.Role;
import com.trackit.entity.RoleCode;
import com.trackit.entity.Utilisateur;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.AnomalieRepository;
import com.trackit.repository.CommentaireRepository;
import com.trackit.repository.HistoriqueRepository;
import com.trackit.repository.NotificationRepository;
import com.trackit.repository.PieceJointeRepository;
import com.trackit.repository.RapportRepository;
import com.trackit.repository.RoleRepository;
import com.trackit.repository.UtilisateurRepository;
import com.trackit.security.JournalSecurite;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.validation.PolitiqueMotDePasse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestion des comptes (UC-04) et du profil de l'utilisateur connecté.
 *
 * <p>Suppression d'un compte : un compte sans aucune trace métier est supprimé physiquement. Un compte
 * lié à des anomalies, commentaires, historiques, pièces jointes ou rapports est supprimé logiquement
 * (soft delete) : ses données personnelles sont anonymisées, il est désactivé et sa date de suppression
 * est renseignée. Les traces restent ainsi cohérentes et conservées, sans données personnelles.
 */
@Service
public class UtilisateurService {

    /** Domaine réservé (RFC 2606) : une adresse anonymisée ne peut appartenir à personne. */
    private static final String DOMAINE_ANONYME = "@trackit.invalid";

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final NotificationRepository notificationRepository;
    private final AnomalieRepository anomalieRepository;
    private final CommentaireRepository commentaireRepository;
    private final HistoriqueRepository historiqueRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final RapportRepository rapportRepository;
    private final PasswordEncoder passwordEncoder;
    private final JournalSecurite journal;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, RoleRepository roleRepository,
                              NotificationRepository notificationRepository, AnomalieRepository anomalieRepository,
                              CommentaireRepository commentaireRepository, HistoriqueRepository historiqueRepository,
                              PieceJointeRepository pieceJointeRepository, RapportRepository rapportRepository,
                              PasswordEncoder passwordEncoder, JournalSecurite journal) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.notificationRepository = notificationRepository;
        this.anomalieRepository = anomalieRepository;
        this.commentaireRepository = commentaireRepository;
        this.historiqueRepository = historiqueRepository;
        this.pieceJointeRepository = pieceJointeRepository;
        this.rapportRepository = rapportRepository;
        this.passwordEncoder = passwordEncoder;
        this.journal = journal;
    }

    // ------------------------------------------------------------------ Administration

    @Transactional(readOnly = true)
    public PageResponse<UtilisateurResponse> lister(UtilisateurConnecte courant, String q, Long idRole, Boolean actif,
                                                    Pageable pageable) {
        Specification<Utilisateur> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("organisation").get("id"), courant.organisationId()));
            // Les comptes supprimés logiquement n'apparaissent plus dans la gestion des comptes.
            p.add(cb.isNull(root.get("dateSuppression")));
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
        if (PolitiqueMotDePasse.contientInformationPersonnelle(requete.motDePasse(), email, requete.nom(), requete.prenom())) {
            throw ApiException.motDePassePersonnel();
        }
        Utilisateur u = new Utilisateur();
        u.setNom(requete.nom().trim().toUpperCase());
        u.setPrenom(requete.prenom().trim());
        u.setEmail(email);
        u.setMotDePasse(passwordEncoder.encode(requete.motDePasse()));
        u.setActif(requete.actif() == null || requete.actif());
        u.setRole(role(requete.idRole()));
        u.setOrganisation(administrateur.getOrganisation());
        utilisateurRepository.save(u);
        journal.operationCompte("COMPTE_CREE", courant.id(), u.getId());
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
        if (u.getRoleCode() != role.getCode()) {
            journal.roleModifie(courant.id(), id, u.getRoleCode().name(), role.getCode().name());
        }
        if (u.isActif() != requete.actif()) {
            journal.operationCompte(requete.actif() ? "COMPTE_REACTIVE" : "COMPTE_DESACTIVE", courant.id(), id);
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
        verifierInformationsPersonnelles(requete.nouveauMotDePasse(), u);
        u.setMotDePasse(passwordEncoder.encode(requete.nouveauMotDePasse()));
        journal.operationCompte("MOT_DE_PASSE_REINITIALISE", courant.id(), id);
    }

    /** Suppression d'un compte par l'administrateur de l'organisation. */
    @Transactional
    public void supprimer(UtilisateurConnecte courant, Long id) {
        if (id.equals(courant.id())) {
            throw ApiException.conflit("Vous ne pouvez pas supprimer votre propre compte.");
        }
        retirer(charger(courant, id), courant.id());
    }

    /** Désinscription : l'utilisateur supprime son propre compte en confirmant son mot de passe. */
    @Transactional
    public void supprimerMonCompte(UtilisateurConnecte courant, SuppressionCompteRequest requete) {
        Utilisateur u = charger(courant, courant.id());
        if (!passwordEncoder.matches(requete.motDePasse(), u.getMotDePasse())) {
            throw ApiException.invalide("Le mot de passe est incorrect.");
        }
        if (u.getRoleCode() == RoleCode.ADMINISTRATEUR
                && utilisateurRepository.countByOrganisationIdAndRoleNomAndActifTrueAndIdNot(
                        courant.organisationId(), RoleCode.ADMINISTRATEUR.getLibelle(), u.getId()) == 0) {
            throw ApiException.conflit("Vous êtes le dernier administrateur actif de votre organisation : "
                    + "attribuez ce rôle à un autre compte avant de supprimer le vôtre.");
        }
        retirer(u, courant.id());
        journal.operationCompte("DESINSCRIPTION", courant.id(), courant.id());
    }

    /** Supprime physiquement un compte sans trace, ou l'anonymise s'il est lié à des données métier. */
    private void retirer(Utilisateur u, Long auteur) {
        Long id = u.getId();
        // Les notifications sont personnelles : elles sont effacées dans les deux cas.
        notificationRepository.deleteByUtilisateurId(id);
        boolean aDesTraces = anomalieRepository.existsByAuteurIdOrOperateurId(id, id)
                || commentaireRepository.existsByUtilisateurId(id)
                || historiqueRepository.existsByUtilisateurId(id)
                || pieceJointeRepository.existsByUtilisateurId(id)
                || rapportRepository.existsByAnalysteId(id);
        if (aDesTraces) {
            anonymiser(u);
            journal.operationCompte("COMPTE_ANONYMISE", auteur, id);
        } else {
            utilisateurRepository.delete(u);
            utilisateurRepository.flush();
            journal.operationCompte("COMPTE_SUPPRIME", auteur, id);
        }
    }

    /** Soft delete : la ligne reste en base pour les clés étrangères, sans aucune donnée personnelle. */
    private void anonymiser(Utilisateur u) {
        u.setNom("SUPPRIMÉ");
        u.setPrenom("Utilisateur");
        u.setEmail("supprime-" + u.getId() + DOMAINE_ANONYME);
        // Mot de passe aléatoire jamais communiqué : plus aucune connexion n'est possible.
        u.setMotDePasse(passwordEncoder.encode(UUID.randomUUID().toString()));
        u.setActif(false);
        u.setDateSuppression(LocalDateTime.now());
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
        verifierInformationsPersonnelles(requete.nouveauMotDePasse(), u);
        u.setMotDePasse(passwordEncoder.encode(requete.nouveauMotDePasse()));
        journal.operationCompte("MOT_DE_PASSE_CHANGE", u.getId(), u.getId());
    }

    /** Règle NIST complémentaire : le mot de passe ne reprend pas l'identité de son titulaire. */
    private void verifierInformationsPersonnelles(String motDePasse, Utilisateur u) {
        if (PolitiqueMotDePasse.contientInformationPersonnelle(motDePasse, u.getEmail(), u.getNom(), u.getPrenom())) {
            throw ApiException.motDePassePersonnel();
        }
    }

    /** Charge un compte de l'organisation ; un compte supprimé logiquement est traité comme introuvable. */
    private Utilisateur charger(UtilisateurConnecte courant, Long id) {
        return utilisateurRepository.findByIdAndOrganisationIdAndDateSuppressionIsNull(id, courant.organisationId())
                .orElseThrow(() -> ApiException.introuvable("Utilisateur introuvable."));
    }

    private Role role(Long idRole) {
        return roleRepository.findById(idRole).orElseThrow(() -> ApiException.invalide("Rôle inconnu."));
    }
}
