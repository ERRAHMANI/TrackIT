package com.trackit.service;

import com.trackit.dto.CommentaireRequest;
import com.trackit.dto.CommentaireResponse;
import com.trackit.entity.Anomalie;
import com.trackit.entity.Commentaire;
import com.trackit.entity.Utilisateur;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.CommentaireRepository;
import com.trackit.repository.UtilisateurRepository;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static com.trackit.mapper.DtoMapper.reference;

@Service
public class CommentaireService {

    private final CommentaireRepository commentaireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AnomalieService anomalieService;
    private final AnomaliePermissions permissions;
    private final NotificationService notificationService;

    public CommentaireService(CommentaireRepository commentaireRepository, UtilisateurRepository utilisateurRepository,
                              AnomalieService anomalieService, AnomaliePermissions permissions,
                              NotificationService notificationService) {
        this.commentaireRepository = commentaireRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.anomalieService = anomalieService;
        this.permissions = permissions;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CommentaireResponse> lister(UtilisateurConnecte courant, Long idAnomalie) {
        anomalieService.charger(courant, idAnomalie);
        return commentaireRepository.findByAnomalieIdOrderByDateCreationAsc(idAnomalie).stream()
                .map(DtoMapper::commentaire).toList();
    }

    @Transactional
    public CommentaireResponse ajouter(UtilisateurConnecte courant, Long idAnomalie, CommentaireRequest requete) {
        Anomalie anomalie = anomalieService.charger(courant, idAnomalie);
        if (!permissions.peutCommenter(courant, anomalie)) {
            throw ApiException.interdit("Vous ne pouvez pas commenter l'anomalie " + reference(idAnomalie)
                    + " (rôle non autorisé ou anomalie fermée).");
        }
        Utilisateur auteur = utilisateurRepository.findWithRoleById(courant.id())
                .orElseThrow(() -> ApiException.introuvable("Utilisateur introuvable."));
        Commentaire commentaire = new Commentaire();
        commentaire.setAnomalie(anomalie);
        commentaire.setUtilisateur(auteur);
        commentaire.setContenu(requete.contenu().trim());
        commentaireRepository.save(commentaire);

        notificationService.notifier(Arrays.asList(anomalie.getAuteur(), anomalie.getOperateur()), auteur,
                anomalie, "Nouveau commentaire de " + auteur.getNomComplet() + " sur l'anomalie " + reference(idAnomalie) + ".");
        return DtoMapper.commentaire(commentaire);
    }
}
