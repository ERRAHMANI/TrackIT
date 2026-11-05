package com.trackit.service;

import com.trackit.dto.SystemeItRequest;
import com.trackit.dto.SystemeItResponse;
import com.trackit.entity.SystemeIt;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.AnomalieRepository;
import com.trackit.repository.OrganisationRepository;
import com.trackit.repository.SystemeItRepository;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SystemeItService {

    private final SystemeItRepository systemeItRepository;
    private final OrganisationRepository organisationRepository;
    private final AnomalieRepository anomalieRepository;

    public SystemeItService(SystemeItRepository systemeItRepository, OrganisationRepository organisationRepository,
                            AnomalieRepository anomalieRepository) {
        this.systemeItRepository = systemeItRepository;
        this.organisationRepository = organisationRepository;
        this.anomalieRepository = anomalieRepository;
    }

    @Transactional(readOnly = true)
    public List<SystemeItResponse> lister(UtilisateurConnecte courant, boolean actifsSeulement) {
        return systemeItRepository.findByOrganisationIdOrderByNomAsc(courant.organisationId()).stream()
                .filter(s -> !actifsSeulement || s.isActif())
                .map(DtoMapper::systeme).toList();
    }

    @Transactional(readOnly = true)
    public SystemeItResponse obtenir(UtilisateurConnecte courant, Long id) {
        return DtoMapper.systeme(charger(courant, id));
    }

    @Transactional
    public SystemeItResponse creer(UtilisateurConnecte courant, SystemeItRequest requete) {
        String nom = requete.nom().trim();
        if (systemeItRepository.existsByOrganisationIdAndNomIgnoreCase(courant.organisationId(), nom)) {
            throw ApiException.conflit("Un système nommé « " + nom + " » existe déjà.");
        }
        SystemeIt systeme = new SystemeIt();
        appliquer(systeme, requete);
        systeme.setOrganisation(organisationRepository.getReferenceById(courant.organisationId()));
        return DtoMapper.systeme(systemeItRepository.save(systeme));
    }

    @Transactional
    public SystemeItResponse modifier(UtilisateurConnecte courant, Long id, SystemeItRequest requete) {
        SystemeIt systeme = charger(courant, id);
        String nom = requete.nom().trim();
        if (systemeItRepository.existsByOrganisationIdAndNomIgnoreCaseAndIdNot(courant.organisationId(), nom, id)) {
            throw ApiException.conflit("Un système nommé « " + nom + " » existe déjà.");
        }
        appliquer(systeme, requete);
        return DtoMapper.systeme(systeme);
    }

    @Transactional
    public void supprimer(UtilisateurConnecte courant, Long id) {
        SystemeIt systeme = charger(courant, id);
        if (anomalieRepository.existsBySystemeId(id)) {
            throw ApiException.conflit("Ce système est référencé par des anomalies : désactivez-le plutôt que de le supprimer.");
        }
        systemeItRepository.delete(systeme);
    }

    private void appliquer(SystemeIt systeme, SystemeItRequest requete) {
        systeme.setNom(requete.nom().trim());
        systeme.setDescription(vide(requete.description()));
        systeme.setAdresse(vide(requete.adresse()));
        systeme.setActif(requete.actif() == null || requete.actif());
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private SystemeIt charger(UtilisateurConnecte courant, Long id) {
        return systemeItRepository.findByIdAndOrganisationId(id, courant.organisationId())
                .orElseThrow(() -> ApiException.introuvable("Système introuvable."));
    }
}
