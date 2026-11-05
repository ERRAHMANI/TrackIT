package com.trackit.service;

import com.trackit.dto.*;
import com.trackit.entity.*;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.*;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.trackit.mapper.DtoMapper.reference;

@Service
public class AnomalieService {

    private final AnomalieRepository anomalieRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SystemeItRepository systemeItRepository;
    private final TypeAnomalieRepository typeAnomalieRepository;
    private final PrioriteRepository prioriteRepository;
    private final StatutRepository statutRepository;
    private final CommentaireRepository commentaireRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final HistoriqueRepository historiqueRepository;
    private final HistoriqueService historiqueService;
    private final NotificationService notificationService;
    private final AnomaliePermissions permissions;

    public AnomalieService(AnomalieRepository anomalieRepository, UtilisateurRepository utilisateurRepository,
                           SystemeItRepository systemeItRepository, TypeAnomalieRepository typeAnomalieRepository,
                           PrioriteRepository prioriteRepository, StatutRepository statutRepository,
                           CommentaireRepository commentaireRepository, PieceJointeRepository pieceJointeRepository,
                           HistoriqueRepository historiqueRepository, HistoriqueService historiqueService,
                           NotificationService notificationService, AnomaliePermissions permissions) {
        this.anomalieRepository = anomalieRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.systemeItRepository = systemeItRepository;
        this.typeAnomalieRepository = typeAnomalieRepository;
        this.prioriteRepository = prioriteRepository;
        this.statutRepository = statutRepository;
        this.commentaireRepository = commentaireRepository;
        this.pieceJointeRepository = pieceJointeRepository;
        this.historiqueRepository = historiqueRepository;
        this.historiqueService = historiqueService;
        this.notificationService = notificationService;
        this.permissions = permissions;
    }

    // ------------------------------------------------------------------ Consultation

    @Transactional(readOnly = true)
    public PageResponse<AnomalieResume> rechercher(UtilisateurConnecte courant, AnomalieCritere critere, Pageable pageable) {
        Page<Anomalie> page = anomalieRepository.findAll(
                AnomalieSpecifications.rechercher(courant.organisationId(), courant.id(), critere), pageable);
        return PageResponse.depuis(page, DtoMapper::anomalieResume);
    }

    @Transactional(readOnly = true)
    public AnomalieDetail obtenir(UtilisateurConnecte courant, Long id) {
        return detail(courant, charger(courant, id));
    }

    /**
     * Charge une anomalie de l'organisation de l'utilisateur. Une anomalie d'une autre
     * organisation est signalée comme introuvable pour ne pas révéler son existence.
     */
    @Transactional(readOnly = true)
    public Anomalie charger(UtilisateurConnecte courant, Long id) {
        Anomalie anomalie = anomalieRepository.findDetailById(id)
                .orElseThrow(() -> ApiException.introuvable("Anomalie " + reference(id) + " introuvable."));
        if (!anomalie.getSysteme().getOrganisation().getId().equals(courant.organisationId())) {
            throw ApiException.introuvable("Anomalie " + reference(id) + " introuvable.");
        }
        return anomalie;
    }

    // ------------------------------------------------------------------ Déclaration et modification

    @Transactional
    public AnomalieDetail declarer(UtilisateurConnecte courant, AnomalieRequest requete) {
        if (!permissions.peutDeclarer(courant)) {
            throw ApiException.interdit("Seul un opérateur peut déclarer une anomalie.");
        }
        Utilisateur auteur = utilisateurCourant(courant);
        Anomalie anomalie = new Anomalie();
        anomalie.setTitre(requete.titre().trim());
        anomalie.setDescription(requete.description().trim());
        anomalie.setSysteme(systemeActif(courant, requete.idSysteme()));
        anomalie.setType(type(requete.idType()));
        anomalie.setPriorite(priorite(requete.idPriorite()));
        anomalie.setStatut(statut(StatutCode.NOUVELLE));
        anomalie.setAuteur(auteur);
        anomalie.setDateCreation(LocalDateTime.now());
        anomalieRepository.save(anomalie);

        historiqueService.journaliserCreation(anomalie, auteur);
        List<Utilisateur> responsables = utilisateurRepository.findByOrganisationIdAndRoleNomAndActifTrueOrderByNomAscPrenomAsc(
                courant.organisationId(), RoleCode.RESPONSABLE_TECHNIQUE.getLibelle());
        notificationService.notifier(responsables, auteur, anomalie,
                "Nouvelle anomalie " + reference(anomalie.getId()) + " « " + anomalie.getTitre() + " » ("
                        + anomalie.getPriorite().getNom() + ") déclarée par " + auteur.getNomComplet() + ".");
        notificationService.creer(auteur, anomalie, "Accusé de réception : votre anomalie " + reference(anomalie.getId())
                + " « " + anomalie.getTitre() + " » a été enregistrée avec le statut Nouvelle.");
        return detail(courant, anomalie);
    }

    @Transactional
    public AnomalieDetail modifier(UtilisateurConnecte courant, Long id, AnomalieRequest requete) {
        Anomalie anomalie = charger(courant, id);
        if (!permissions.peutModifier(courant, anomalie)) {
            throw refus(anomalie, "modifier");
        }
        Utilisateur auteur = utilisateurCourant(courant);
        List<String> changements = new ArrayList<>();

        String titre = requete.titre().trim();
        if (!titre.equals(anomalie.getTitre())) {
            anomalie.setTitre(titre);
            changements.add("titre");
        }
        String description = requete.description().trim();
        if (!description.equals(anomalie.getDescription())) {
            anomalie.setDescription(description);
            changements.add("description");
        }
        if (!anomalie.getSysteme().getId().equals(requete.idSysteme())) {
            SystemeIt systeme = systemeActif(courant, requete.idSysteme());
            changements.add("système (" + anomalie.getSysteme().getNom() + " → " + systeme.getNom() + ")");
            anomalie.setSysteme(systeme);
        }
        if (!anomalie.getType().getId().equals(requete.idType())) {
            TypeAnomalie type = type(requete.idType());
            changements.add("type (" + anomalie.getType().getNom() + " → " + type.getNom() + ")");
            anomalie.setType(type);
        }
        if (!changements.isEmpty()) {
            historiqueService.journaliser(anomalie, auteur, "Modification : " + String.join(", ", changements));
        }
        if (!anomalie.getPriorite().getId().equals(requete.idPriorite())) {
            appliquerPriorite(anomalie, priorite(requete.idPriorite()), auteur);
        }
        return detail(courant, anomalie);
    }

    @Transactional
    public AnomalieDetail changerPriorite(UtilisateurConnecte courant, Long id, ChangementPrioriteRequest requete) {
        Anomalie anomalie = charger(courant, id);
        if (!permissions.peutPrioriser(courant, anomalie)) {
            throw refus(anomalie, "changer la priorité de");
        }
        Priorite nouvelle = priorite(requete.idPriorite());
        if (nouvelle.getId().equals(anomalie.getPriorite().getId())) {
            throw ApiException.conflit("L'anomalie a déjà la priorité « " + nouvelle.getNom() + " ».");
        }
        Utilisateur auteur = utilisateurCourant(courant);
        appliquerPriorite(anomalie, nouvelle, auteur);
        ajouterCommentaireFacultatif(anomalie, auteur, requete.commentaire());
        return detail(courant, anomalie);
    }

    private void appliquerPriorite(Anomalie anomalie, Priorite nouvelle, Utilisateur auteur) {
        String ancienne = anomalie.getPriorite().getNom();
        anomalie.setPriorite(nouvelle);
        historiqueService.journaliserPriorite(anomalie, auteur, "Changement de priorité", ancienne, nouvelle.getNom());
        notificationService.notifier(List.of(anomalie.getAuteur(), Objects.requireNonNullElse(anomalie.getOperateur(), anomalie.getAuteur())),
                auteur, anomalie, "La priorité de l'anomalie " + reference(anomalie.getId()) + " est passée de « "
                        + ancienne + " » à « " + nouvelle.getNom() + " ».");
    }

    // ------------------------------------------------------------------ Cycle de vie

    @Transactional
    public AnomalieDetail changerStatut(UtilisateurConnecte courant, Long id, ChangementStatutRequest requete) {
        Anomalie anomalie = charger(courant, id);
        Statut cible = statutRepository.findById(requete.idStatut())
                .orElseThrow(() -> ApiException.invalide("Statut inconnu."));
        StatutCode actuel = anomalie.getStatutCode();
        StatutCode nouveau = cible.getCode();

        if (actuel == nouveau) {
            throw ApiException.conflit("L'anomalie est déjà au statut « " + cible.getNom() + " ».");
        }
        if (!actuel.peutPasserA(nouveau)) {
            throw ApiException.conflit("Transition non autorisée : « " + actuel.getLibelle() + " » → « "
                    + nouveau.getLibelle() + " ».");
        }
        if (!permissions.peutPasserA(courant, anomalie, nouveau)) {
            if (nouveau == StatutCode.EN_COURS && courant.role() == RoleCode.RESPONSABLE_TECHNIQUE) {
                throw ApiException.conflit("Affectez d'abord un opérateur avant de passer l'anomalie « En cours ».");
            }
            throw refus(anomalie, "passer au statut « " + nouveau.getLibelle() + " »");
        }

        Utilisateur auteur = utilisateurCourant(courant);
        if (nouveau == StatutCode.EN_COURS && anomalie.getOperateur() == null) {
            // Un opérateur qui démarre le traitement d'une anomalie non affectée la prend en charge.
            anomalie.setOperateur(auteur);
            historiqueService.journaliser(anomalie, auteur, "Prise en charge par " + auteur.getNomComplet());
        }
        if (nouveau == StatutCode.RESOLUE) {
            anomalie.setDateResolution(LocalDateTime.now());
        } else if (nouveau == StatutCode.EN_COURS) {
            anomalie.setDateResolution(null);
        }
        anomalie.setStatut(cible);

        String action = actuel == StatutCode.RESOLUE && nouveau == StatutCode.EN_COURS
                ? "Réouverture de l'anomalie" : "Changement de statut";
        historiqueService.journaliserStatut(anomalie, auteur, action, actuel.getLibelle(), nouveau.getLibelle());
        ajouterCommentaireFacultatif(anomalie, auteur, requete.commentaire());
        notificationService.notifier(List.of(anomalie.getAuteur(), Objects.requireNonNullElse(anomalie.getOperateur(), anomalie.getAuteur())),
                auteur, anomalie, "L'anomalie " + reference(anomalie.getId()) + " est passée du statut « "
                        + actuel.getLibelle() + " » au statut « " + nouveau.getLibelle() + " » (" + auteur.getNomComplet() + ").");
        return detail(courant, anomalie);
    }

    @Transactional
    public AnomalieDetail affecter(UtilisateurConnecte courant, Long id, AffectationRequest requete) {
        Anomalie anomalie = charger(courant, id);
        boolean priseEnCharge = courant.role() == RoleCode.OPERATEUR;
        if (priseEnCharge) {
            if (!requete.idOperateur().equals(courant.id())) {
                throw ApiException.interdit("Un opérateur ne peut affecter une anomalie qu'à lui-même. "
                        + "La réaffectation relève du responsable technique.");
            }
            if (!permissions.peutPrendreEnCharge(courant, anomalie)) {
                throw ApiException.conflit("Cette anomalie est déjà affectée ou n'est plus ouverte.");
            }
        } else if (!permissions.peutAffecter(courant, anomalie)) {
            if (courant.role() == RoleCode.RESPONSABLE_TECHNIQUE) {
                throw ApiException.conflit("Seule une anomalie Nouvelle ou En cours peut être affectée.");
            }
            throw refus(anomalie, "affecter");
        }

        Utilisateur operateur = utilisateurRepository.findByIdAndOrganisationId(requete.idOperateur(), courant.organisationId())
                .orElseThrow(() -> ApiException.invalide("Opérateur introuvable dans votre organisation."));
        if (operateur.getRoleCode() != RoleCode.OPERATEUR || !operateur.isActif()) {
            throw ApiException.invalide("L'utilisateur choisi n'est pas un opérateur actif.");
        }
        Utilisateur precedent = anomalie.getOperateur();
        if (precedent != null && precedent.getId().equals(operateur.getId())) {
            throw ApiException.conflit("L'anomalie est déjà affectée à " + operateur.getNomComplet() + ".");
        }

        Utilisateur auteur = utilisateurCourant(courant);
        anomalie.setOperateur(operateur);
        String action = priseEnCharge ? "Prise en charge par " + operateur.getNomComplet()
                : precedent == null ? "Affectation à " + operateur.getNomComplet() + " (Opérateur)"
                : "Réaffectation : " + precedent.getNomComplet() + " → " + operateur.getNomComplet();
        historiqueService.journaliser(anomalie, auteur, action);
        ajouterCommentaireFacultatif(anomalie, auteur, requete.commentaire());

        notificationService.notifier(List.of(operateur), auteur, anomalie, "L'anomalie " + reference(anomalie.getId())
                + " « " + anomalie.getTitre() + " » vous a été affectée par " + auteur.getNomComplet() + ".");
        if (precedent != null) {
            notificationService.notifier(List.of(precedent), auteur, anomalie, "L'anomalie " + reference(anomalie.getId())
                    + " a été réaffectée à " + operateur.getNomComplet() + ".");
        }
        notificationService.notifier(List.of(anomalie.getAuteur()), auteur, anomalie, "Votre anomalie "
                + reference(anomalie.getId()) + " est prise en charge par " + operateur.getNomComplet() + ".");
        return detail(courant, anomalie);
    }

    @Transactional
    public void supprimer(UtilisateurConnecte courant, Long id) {
        Anomalie anomalie = charger(courant, id);
        if (!permissions.peutSupprimer(courant)) {
            throw refus(anomalie, "supprimer");
        }
        anomalieRepository.delete(anomalie);
    }

    // ------------------------------------------------------------------ Outils

    private void ajouterCommentaireFacultatif(Anomalie anomalie, Utilisateur auteur, String contenu) {
        if (contenu != null && !contenu.isBlank()) {
            Commentaire commentaire = new Commentaire();
            commentaire.setAnomalie(anomalie);
            commentaire.setUtilisateur(auteur);
            commentaire.setContenu(contenu.trim());
            commentaireRepository.save(commentaire);
        }
    }

    private AnomalieDetail detail(UtilisateurConnecte courant, Anomalie a) {
        List<StatutResponse> statutsPossibles = permissions.statutsPossibles(courant, a).stream()
                .map(this::statut).map(DtoMapper::statut).toList();
        ActionsAutorisees actions = new ActionsAutorisees(
                permissions.peutModifier(courant, a),
                !statutsPossibles.isEmpty(),
                statutsPossibles,
                permissions.peutPrioriser(courant, a),
                permissions.peutAffecter(courant, a),
                permissions.peutPrendreEnCharge(courant, a),
                permissions.peutCommenter(courant, a),
                permissions.peutJoindreFichier(courant, a),
                permissions.peutSupprimer(courant));
        List<Historique> evenements = a.getId() == null ? List.of()
                : historiqueRepository.findByAnomalieIdOrderByDateModificationAscIdAsc(a.getId());
        LocalDateTime derniereMiseAJour = evenements.isEmpty() ? a.getDateCreation()
                : evenements.get(evenements.size() - 1).getDateModification();
        return new AnomalieDetail(a.getId(), a.getTitre(), a.getDescription(),
                new RefResponse(a.getSysteme().getId(), a.getSysteme().getNom()),
                new RefResponse(a.getType().getId(), a.getType().getNom()),
                DtoMapper.priorite(a.getPriorite()), DtoMapper.statut(a.getStatut()),
                DtoMapper.resume(a.getAuteur()), DtoMapper.resume(a.getOperateur()),
                a.getSysteme().getOrganisation().getNom(),
                a.getDateCreation(), a.getDateResolution(), derniereMiseAJour,
                commentaireRepository.countByAnomalieId(a.getId()),
                pieceJointeRepository.countByAnomalieId(a.getId()),
                evenements.size(),
                actions);
    }

    private ApiException refus(Anomalie anomalie, String action) {
        return ApiException.interdit("Vous n'êtes pas autorisé à " + action + " l'anomalie " + reference(anomalie.getId()) + ".");
    }

    private Utilisateur utilisateurCourant(UtilisateurConnecte courant) {
        return utilisateurRepository.findWithRoleById(courant.id())
                .orElseThrow(() -> ApiException.introuvable("Utilisateur introuvable."));
    }

    private SystemeIt systemeActif(UtilisateurConnecte courant, Long idSysteme) {
        SystemeIt systeme = systemeItRepository.findByIdAndOrganisationId(idSysteme, courant.organisationId())
                .orElseThrow(() -> ApiException.invalide("Système introuvable : le système concerné n'existe pas dans votre organisation."));
        if (!systeme.isActif()) {
            throw ApiException.invalide("Le système « " + systeme.getNom() + " » est désactivé.");
        }
        return systeme;
    }

    private TypeAnomalie type(Long id) {
        return typeAnomalieRepository.findById(id).orElseThrow(() -> ApiException.invalide("Type d'anomalie inconnu."));
    }

    private Priorite priorite(Long id) {
        return prioriteRepository.findById(id).orElseThrow(() -> ApiException.invalide("Priorité inconnue."));
    }

    private Statut statut(StatutCode code) {
        return statutRepository.findByNom(code.getLibelle())
                .orElseThrow(() -> new IllegalStateException("Statut absent du référentiel : " + code.getLibelle()));
    }
}
