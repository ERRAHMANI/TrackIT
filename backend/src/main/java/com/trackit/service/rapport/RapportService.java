package com.trackit.service.rapport;

import com.trackit.config.TrackItProperties;
import com.trackit.dto.*;
import com.trackit.entity.FormatRapport;
import com.trackit.entity.Rapport;
import com.trackit.entity.Utilisateur;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.AnomalieRepository;
import com.trackit.repository.AnomalieSpecifications;
import com.trackit.repository.RapportRepository;
import com.trackit.repository.UtilisateurRepository;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.service.AbonnementService;
import com.trackit.service.StatistiqueService;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Génération, historisation et téléchargement des rapports (UC-03). */
@Service
public class RapportService {

    private static final int MAX_LIGNES_LISTE = 5000;

    private final RapportRepository rapportRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AnomalieRepository anomalieRepository;
    private final StatistiqueService statistiqueService;
    private final AbonnementService abonnementService;
    private final PdfRapportGenerateur pdf;
    private final ExcelRapportGenerateur excel;
    private final Path racine;

    public RapportService(RapportRepository rapportRepository, UtilisateurRepository utilisateurRepository,
                          AnomalieRepository anomalieRepository, StatistiqueService statistiqueService,
                          AbonnementService abonnementService, PdfRapportGenerateur pdf, ExcelRapportGenerateur excel,
                          TrackItProperties properties) {
        this.rapportRepository = rapportRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.anomalieRepository = anomalieRepository;
        this.statistiqueService = statistiqueService;
        this.abonnementService = abonnementService;
        this.pdf = pdf;
        this.excel = excel;
        this.racine = Path.of(properties.storage().reportsDir()).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public PageResponse<RapportResponse> lister(UtilisateurConnecte courant, int page, int taille) {
        return PageResponse.depuis(rapportRepository.findByAnalysteOrganisationIdOrderByDateGenerationDesc(
                courant.organisationId(), PageRequest.of(page, taille)), this::reponse);
    }

    @Transactional
    public RapportResponse generer(UtilisateurConnecte courant, RapportRequest requete) {
        if (!abonnementService.estPremium(courant.organisationId())) {
            throw ApiException.premiumRequis();
        }
        Utilisateur analyste = utilisateurRepository.findWithRoleById(courant.id())
                .orElseThrow(() -> ApiException.introuvable("Utilisateur introuvable."));
        StatistiquesResponse statistiques = statistiqueService.statistiques(courant, requete.dateDebut(), requete.dateFin());
        List<AnomalieResume> anomalies = requete.type() == TypeRapport.LISTE_ANOMALIES
                ? anomaliesDeLaPeriode(courant, requete) : List.of();

        LocalDateTime maintenant = LocalDateTime.now();
        ContenuRapport contenu = new ContenuRapport(requete.titre().trim(), requete.type(),
                analyste.getOrganisation().getNom(), analyste.getNomComplet(), maintenant, statistiques, anomalies);
        byte[] octets = requete.format() == FormatRapport.PDF ? pdf.generer(contenu) : excel.generer(contenu);

        String cheminRelatif = "org-" + courant.organisationId() + "/" + UUID.randomUUID() + "." + requete.format().getExtension();
        try {
            Path fichier = racine.resolve(cheminRelatif);
            Files.createDirectories(fichier.getParent());
            Files.write(fichier, octets);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'enregistrer le rapport", e);
        }

        Rapport rapport = new Rapport();
        rapport.setTitre(requete.titre().trim());
        rapport.setType(requete.type().getLibelle());
        rapport.setFormat(requete.format());
        rapport.setDateGeneration(maintenant);
        rapport.setCheminFichier(cheminRelatif);
        rapport.setAnalyste(analyste);
        return reponse(rapportRepository.save(rapport));
    }

    public record FichierRapport(Resource ressource, String nomFichier, String typeContenu) {
    }

    @Transactional(readOnly = true)
    public FichierRapport telecharger(UtilisateurConnecte courant, Long id) {
        Rapport rapport = rapportRepository.findWithAnalysteById(id)
                .filter(r -> r.getAnalyste().getOrganisation().getId().equals(courant.organisationId()))
                .orElseThrow(() -> ApiException.introuvable("Rapport introuvable."));
        Path fichier = fichier(rapport);
        if (fichier == null) {
            throw ApiException.introuvable("Le fichier de ce rapport n'est plus disponible : générez-le à nouveau.");
        }
        String nom = rapport.getTitre().replaceAll("[^\\p{L}\\p{N} _-]", "").trim().replace(' ', '_')
                + "." + rapport.getFormat().getExtension();
        return new FichierRapport(new PathResource(fichier), nom, rapport.getFormat().getContentType());
    }

    private List<AnomalieResume> anomaliesDeLaPeriode(UtilisateurConnecte courant, RapportRequest requete) {
        AnomalieCritere critere = new AnomalieCritere(null, null, null, null, null, null, null, null, null,
                requete.dateDebut(), requete.dateFin());
        return anomalieRepository.findAll(AnomalieSpecifications.rechercher(courant.organisationId(), courant.id(), critere),
                        PageRequest.of(0, MAX_LIGNES_LISTE, Sort.by("dateCreation")))
                .map(DtoMapper::anomalieResume).getContent();
    }

    private Path fichier(Rapport rapport) {
        if (rapport.getCheminFichier() == null) {
            return null;
        }
        Path fichier = racine.resolve(rapport.getCheminFichier()).normalize();
        return fichier.startsWith(racine) && Files.isRegularFile(fichier) ? fichier : null;
    }

    private RapportResponse reponse(Rapport r) {
        return new RapportResponse(r.getId(), r.getTitre(), r.getType(), r.getFormat().name(), r.getDateGeneration(),
                fichier(r) != null, new UtilisateurResume(r.getAnalyste().getId(), r.getAnalyste().getNomComplet(), null));
    }
}
