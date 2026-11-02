package com.trackit.mapper;

import com.trackit.dto.*;
import com.trackit.entity.*;

/** Conversions entité → DTO. Les méthodes doivent être appelées dans une transaction ouverte. */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static String reference(Long idAnomalie) {
        return String.format("#%03d", idAnomalie);
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

    public static RoleResponse role(Role r) {
        return new RoleResponse(r.getId(), r.getNom(), r.getCode().name(), r.getDescription());
    }

    public static SystemeItResponse systeme(SystemeIt s) {
        return new SystemeItResponse(s.getId(), s.getNom(), s.getDescription(), s.getAdresse(), s.isActif());
    }

    public static TypeAnomalieResponse type(TypeAnomalie t) {
        return new TypeAnomalieResponse(t.getId(), t.getNom(), t.getDescription());
    }

    public static PrioriteResponse priorite(Priorite p) {
        return new PrioriteResponse(p.getId(), p.getNom(), p.getNiveau(), p.getDescription());
    }

    public static StatutResponse statut(Statut s) {
        return new StatutResponse(s.getId(), s.getNom(), s.getCode().name(), s.getDescription());
    }

    public static AnomalieResume anomalieResume(Anomalie a) {
        return new AnomalieResume(a.getId(), a.getTitre(),
                new RefResponse(a.getSysteme().getId(), a.getSysteme().getNom()),
                new RefResponse(a.getType().getId(), a.getType().getNom()),
                priorite(a.getPriorite()), statut(a.getStatut()),
                resumeSansRole(a.getAuteur()), resumeSansRole(a.getOperateur()),
                a.getDateCreation(), a.getDateResolution());
    }

    /** Résumé sans charger le rôle (évite une requête par ligne dans les listes). */
    private static UtilisateurResume resumeSansRole(Utilisateur u) {
        return u == null ? null : new UtilisateurResume(u.getId(), u.getNomComplet(), null);
    }

    public static CommentaireResponse commentaire(Commentaire c) {
        return new CommentaireResponse(c.getId(), c.getContenu(), c.getDateCreation(), resume(c.getUtilisateur()));
    }

    public static HistoriqueResponse historique(Historique h) {
        return new HistoriqueResponse(h.getId(), h.getAction(), h.getAncienStatut(), h.getNouveauStatut(),
                h.getAnciennePriorite(), h.getNouvellePriorite(), h.getDateModification(), resume(h.getUtilisateur()));
    }

    public static PieceJointeResponse pieceJointe(PieceJointe p) {
        return new PieceJointeResponse(p.getId(), p.getNomFichier(), p.getTypeFichier(), p.getDateAjout(),
                resumeSansRole(p.getUtilisateur()));
    }

    public static NotificationResponse notification(Notification n) {
        return new NotificationResponse(n.getId(), n.getMessage(), n.getDateCreation(), n.isLu(),
                n.getAnomalie() == null ? null : n.getAnomalie().getId());
    }

    public static AbonnementResponse abonnement(Abonnement a) {
        return a == null ? null
                : new AbonnementResponse(a.getId(), a.getOffre(), a.getDateDebut(), a.getDateFin(), a.getStatut().name());
    }

    public static PaiementResponse paiement(Paiement p) {
        return new PaiementResponse(p.getId(), p.getMontant(), p.getDevise(), p.getDatePaiement(),
                p.getStatut().name(), p.getReferenceStripe(), p.getAbonnement().getId());
    }
}
