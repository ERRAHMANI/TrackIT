package com.trackit.service;

import com.trackit.dto.*;
import com.trackit.entity.Organisation;
import com.trackit.entity.Priorite;
import com.trackit.entity.TypeAnomalie;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.OrganisationRepository;
import com.trackit.repository.PrioriteRepository;
import com.trackit.repository.RoleRepository;
import com.trackit.repository.StatutRepository;
import com.trackit.repository.TypeAnomalieRepository;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Référentiels (UC-04 « Configurer … ») : rôles et statuts communs en lecture seule, types d'anomalie
 * et priorités propres à chaque organisation. Comme pour les anomalies, une valeur d'une autre
 * organisation est signalée comme introuvable (404) pour ne pas révéler son existence.
 */
@Service
public class ReferentielService {

    /** Valeurs créées pour chaque nouvelle organisation (mêmes valeurs que la migration V2). */
    private static final List<String[]> TYPES_PAR_DEFAUT = List.of(
            new String[]{"Réseau", "Connectivité, VPN, Wi-Fi, DNS, équipements réseau"},
            new String[]{"Application", "Dysfonctionnement d'une application métier"},
            new String[]{"Matériel", "Poste de travail, imprimante, serveur physique, périphérique"},
            new String[]{"Accès", "Authentification, droits, comptes verrouillés"},
            new String[]{"Base de données", "Erreur, lenteur ou indisponibilité d'une base de données"},
            new String[]{"Performance", "Temps de réponse dégradé, saturation de ressources"},
            new String[]{"Fonctionnel", "Comportement non conforme aux règles de gestion"},
            new String[]{"Autre", "Anomalie ne relevant d'aucune autre catégorie"});

    private static final List<String[]> PRIORITES_PAR_DEFAUT = List.of(
            new String[]{"Basse", "1", "Gêne mineure, contournement possible"},
            new String[]{"Moyenne", "2", "Fonctionnalité dégradée pour une partie des utilisateurs"},
            new String[]{"Haute", "3", "Fonctionnalité importante indisponible"},
            new String[]{"Critique", "4", "Service essentiel interrompu, impact sur l'activité"});

    private final RoleRepository roleRepository;
    private final TypeAnomalieRepository typeRepository;
    private final PrioriteRepository prioriteRepository;
    private final StatutRepository statutRepository;
    private final OrganisationRepository organisationRepository;

    public ReferentielService(RoleRepository roleRepository, TypeAnomalieRepository typeRepository,
                              PrioriteRepository prioriteRepository, StatutRepository statutRepository,
                              OrganisationRepository organisationRepository) {
        this.roleRepository = roleRepository;
        this.typeRepository = typeRepository;
        this.prioriteRepository = prioriteRepository;
        this.statutRepository = statutRepository;
        this.organisationRepository = organisationRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roleRepository.findAll(Sort.by("id")).stream().map(DtoMapper::role).toList();
    }

    /** Crée les types et priorités par défaut d'une organisation qui vient de s'inscrire. */
    @Transactional
    public void creerReferentielsParDefaut(Organisation organisation) {
        for (String[] valeur : TYPES_PAR_DEFAUT) {
            TypeAnomalie type = new TypeAnomalie();
            type.setOrganisation(organisation);
            type.setNom(valeur[0]);
            type.setDescription(valeur[1]);
            typeRepository.save(type);
        }
        for (String[] valeur : PRIORITES_PAR_DEFAUT) {
            Priorite priorite = new Priorite();
            priorite.setOrganisation(organisation);
            priorite.setNom(valeur[0]);
            priorite.setNiveau(Integer.valueOf(valeur[1]));
            priorite.setDescription(valeur[2]);
            prioriteRepository.save(priorite);
        }
    }

    // ------------------------------------------------------------------ Types

    @Transactional(readOnly = true)
    public List<TypeAnomalieResponse> types(UtilisateurConnecte courant) {
        return typeRepository.findByOrganisationIdOrderByNomAsc(courant.organisationId()).stream()
                .map(DtoMapper::type).toList();
    }

    @Transactional
    public TypeAnomalieResponse creerType(UtilisateurConnecte courant, TypeAnomalieRequest requete) {
        String nom = requete.nom().trim();
        if (typeRepository.existsByOrganisationIdAndNomIgnoreCase(courant.organisationId(), nom)) {
            throw ApiException.conflit("Le type « " + nom + " » existe déjà.");
        }
        TypeAnomalie type = new TypeAnomalie();
        type.setOrganisation(organisationRepository.getReferenceById(courant.organisationId()));
        type.setNom(nom);
        type.setDescription(requete.description());
        return DtoMapper.type(typeRepository.save(type));
    }

    @Transactional
    public TypeAnomalieResponse modifierType(UtilisateurConnecte courant, Long id, TypeAnomalieRequest requete) {
        TypeAnomalie type = chargerType(courant, id);
        String nom = requete.nom().trim();
        if (typeRepository.existsByOrganisationIdAndNomIgnoreCaseAndIdNot(courant.organisationId(), nom, id)) {
            throw ApiException.conflit("Le type « " + nom + " » existe déjà.");
        }
        type.setNom(nom);
        type.setDescription(requete.description());
        return DtoMapper.type(type);
    }

    @Transactional
    public void supprimerType(UtilisateurConnecte courant, Long id) {
        TypeAnomalie type = chargerType(courant, id);
        supprimerSiInutilise(() -> {
            typeRepository.delete(type);
            typeRepository.flush();
        }, "Ce type est utilisé par des anomalies et ne peut pas être supprimé.");
    }

    private TypeAnomalie chargerType(UtilisateurConnecte courant, Long id) {
        return typeRepository.findByIdAndOrganisationId(id, courant.organisationId())
                .orElseThrow(() -> ApiException.introuvable("Type introuvable."));
    }

    // ------------------------------------------------------------------ Priorités

    @Transactional(readOnly = true)
    public List<PrioriteResponse> priorites(UtilisateurConnecte courant) {
        return prioriteRepository.findByOrganisationIdOrderByNiveauAsc(courant.organisationId()).stream()
                .map(DtoMapper::priorite).toList();
    }

    @Transactional
    public PrioriteResponse creerPriorite(UtilisateurConnecte courant, PrioriteRequest requete) {
        String nom = requete.nom().trim();
        if (prioriteRepository.existsByOrganisationIdAndNomIgnoreCase(courant.organisationId(), nom)) {
            throw ApiException.conflit("La priorité « " + nom + " » existe déjà.");
        }
        Priorite priorite = new Priorite();
        priorite.setOrganisation(organisationRepository.getReferenceById(courant.organisationId()));
        appliquer(priorite, requete);
        return DtoMapper.priorite(prioriteRepository.save(priorite));
    }

    @Transactional
    public PrioriteResponse modifierPriorite(UtilisateurConnecte courant, Long id, PrioriteRequest requete) {
        Priorite priorite = chargerPriorite(courant, id);
        String nom = requete.nom().trim();
        if (prioriteRepository.existsByOrganisationIdAndNomIgnoreCaseAndIdNot(courant.organisationId(), nom, id)) {
            throw ApiException.conflit("La priorité « " + nom + " » existe déjà.");
        }
        appliquer(priorite, requete);
        return DtoMapper.priorite(priorite);
    }

    @Transactional
    public void supprimerPriorite(UtilisateurConnecte courant, Long id) {
        Priorite priorite = chargerPriorite(courant, id);
        supprimerSiInutilise(() -> {
            prioriteRepository.delete(priorite);
            prioriteRepository.flush();
        }, "Cette priorité est utilisée par des anomalies et ne peut pas être supprimée.");
    }

    private Priorite chargerPriorite(UtilisateurConnecte courant, Long id) {
        return prioriteRepository.findByIdAndOrganisationId(id, courant.organisationId())
                .orElseThrow(() -> ApiException.introuvable("Priorité introuvable."));
    }

    private static void appliquer(Priorite priorite, PrioriteRequest requete) {
        priorite.setNom(requete.nom().trim());
        priorite.setNiveau(requete.niveau());
        priorite.setDescription(requete.description());
    }

    // ------------------------------------------------------------------ Statuts (communs et fixes)

    /**
     * Les quatre statuts portent le cycle de vie de l'application (StatutCode) et sont communs à toutes
     * les organisations : ils sont en lecture seule, aucun client ne peut les modifier.
     */
    @Transactional(readOnly = true)
    public List<StatutResponse> statuts() {
        return statutRepository.findAll(Sort.by("id")).stream().map(DtoMapper::statut).toList();
    }

    private static void supprimerSiInutilise(Runnable suppression, String messageConflit) {
        try {
            suppression.run();
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflit(messageConflit);
        }
    }
}
