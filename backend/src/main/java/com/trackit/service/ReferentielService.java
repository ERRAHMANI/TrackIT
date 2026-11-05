package com.trackit.service;

import com.trackit.dto.*;
import com.trackit.entity.Priorite;
import com.trackit.entity.Statut;
import com.trackit.entity.TypeAnomalie;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.PrioriteRepository;
import com.trackit.repository.RoleRepository;
import com.trackit.repository.StatutRepository;
import com.trackit.repository.TypeAnomalieRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Référentiels : rôles (lecture), types, priorités et statuts (UC-04 « Configurer … »). */
@Service
public class ReferentielService {

    private final RoleRepository roleRepository;
    private final TypeAnomalieRepository typeRepository;
    private final PrioriteRepository prioriteRepository;
    private final StatutRepository statutRepository;

    public ReferentielService(RoleRepository roleRepository, TypeAnomalieRepository typeRepository,
                              PrioriteRepository prioriteRepository, StatutRepository statutRepository) {
        this.roleRepository = roleRepository;
        this.typeRepository = typeRepository;
        this.prioriteRepository = prioriteRepository;
        this.statutRepository = statutRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roleRepository.findAll(Sort.by("id")).stream().map(DtoMapper::role).toList();
    }

    // ------------------------------------------------------------------ Types

    @Transactional(readOnly = true)
    public List<TypeAnomalieResponse> types() {
        return typeRepository.findAll(Sort.by("nom")).stream().map(DtoMapper::type).toList();
    }

    @Transactional
    public TypeAnomalieResponse creerType(TypeAnomalieRequest requete) {
        if (typeRepository.existsByNomIgnoreCase(requete.nom().trim())) {
            throw ApiException.conflit("Le type « " + requete.nom().trim() + " » existe déjà.");
        }
        TypeAnomalie type = new TypeAnomalie();
        type.setNom(requete.nom().trim());
        type.setDescription(requete.description());
        return DtoMapper.type(typeRepository.save(type));
    }

    @Transactional
    public TypeAnomalieResponse modifierType(Long id, TypeAnomalieRequest requete) {
        TypeAnomalie type = typeRepository.findById(id).orElseThrow(() -> ApiException.introuvable("Type introuvable."));
        if (typeRepository.existsByNomIgnoreCaseAndIdNot(requete.nom().trim(), id)) {
            throw ApiException.conflit("Le type « " + requete.nom().trim() + " » existe déjà.");
        }
        type.setNom(requete.nom().trim());
        type.setDescription(requete.description());
        return DtoMapper.type(type);
    }

    @Transactional
    public void supprimerType(Long id) {
        TypeAnomalie type = typeRepository.findById(id).orElseThrow(() -> ApiException.introuvable("Type introuvable."));
        supprimerSiInutilise(() -> {
            typeRepository.delete(type);
            typeRepository.flush();
        }, "Ce type est utilisé par des anomalies et ne peut pas être supprimé.");
    }

    // ------------------------------------------------------------------ Priorités

    @Transactional(readOnly = true)
    public List<PrioriteResponse> priorites() {
        return prioriteRepository.findAllByOrderByNiveauAsc().stream().map(DtoMapper::priorite).toList();
    }

    @Transactional
    public PrioriteResponse creerPriorite(PrioriteRequest requete) {
        if (prioriteRepository.existsByNomIgnoreCase(requete.nom().trim())) {
            throw ApiException.conflit("La priorité « " + requete.nom().trim() + " » existe déjà.");
        }
        Priorite priorite = new Priorite();
        appliquer(priorite, requete);
        return DtoMapper.priorite(prioriteRepository.save(priorite));
    }

    @Transactional
    public PrioriteResponse modifierPriorite(Long id, PrioriteRequest requete) {
        Priorite priorite = prioriteRepository.findById(id).orElseThrow(() -> ApiException.introuvable("Priorité introuvable."));
        if (prioriteRepository.existsByNomIgnoreCaseAndIdNot(requete.nom().trim(), id)) {
            throw ApiException.conflit("La priorité « " + requete.nom().trim() + " » existe déjà.");
        }
        appliquer(priorite, requete);
        return DtoMapper.priorite(priorite);
    }

    @Transactional
    public void supprimerPriorite(Long id) {
        Priorite priorite = prioriteRepository.findById(id).orElseThrow(() -> ApiException.introuvable("Priorité introuvable."));
        supprimerSiInutilise(() -> {
            prioriteRepository.delete(priorite);
            prioriteRepository.flush();
        }, "Cette priorité est utilisée par des anomalies et ne peut pas être supprimée.");
    }

    private static void appliquer(Priorite priorite, PrioriteRequest requete) {
        priorite.setNom(requete.nom().trim());
        priorite.setNiveau(requete.niveau());
        priorite.setDescription(requete.description());
    }

    // ------------------------------------------------------------------ Statuts (fixes)

    @Transactional(readOnly = true)
    public List<StatutResponse> statuts() {
        return statutRepository.findAll(Sort.by("id")).stream().map(DtoMapper::statut).toList();
    }

    @Transactional
    public StatutResponse modifierStatut(Long id, StatutRequest requete) {
        Statut statut = statutRepository.findById(id).orElseThrow(() -> ApiException.introuvable("Statut introuvable."));
        statut.setDescription(requete.description());
        return DtoMapper.statut(statut);
    }

    private static void supprimerSiInutilise(Runnable suppression, String messageConflit) {
        try {
            suppression.run();
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflit(messageConflit);
        }
    }
}
