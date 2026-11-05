package com.trackit.service;

import com.trackit.dto.*;
import com.trackit.entity.StatutCode;
import com.trackit.exception.ApiException;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.AnomalieRepository;
import com.trackit.repository.AnomalieSpecifications;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Indicateurs calculés en base (agrégations SQL) pour le tableau de bord et l'analyste. */
@Service
public class StatistiqueService {

    private static final int DUREE_MAX_JOURS = 366;
    private static final long SEUIL_RECURRENCE = 3;

    private final AnomalieRepository anomalieRepository;

    public StatistiqueService(AnomalieRepository anomalieRepository) {
        this.anomalieRepository = anomalieRepository;
    }

    @Transactional(readOnly = true)
    public TableauDeBordResponse tableauDeBord(UtilisateurConnecte courant, LocalDate debut, LocalDate fin) {
        Periode p = periode(debut, fin);
        Long org = courant.organisationId();

        Map<String, Long> actuel = parLibelle(anomalieRepository.compterParStatut(org, p.debut(), p.fin()));
        Periode precedente = p.precedente();
        Map<String, Long> avant = parLibelle(anomalieRepository.compterParStatut(org, precedente.debut(), precedente.fin()));

        List<AnomalieResume> recentes = anomalieRepository.findAll(
                        AnomalieSpecifications.rechercher(org, courant.id(), new AnomalieCritere(null, null, null, null, null,
                                null, null, null, null, null, null)),
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dateCreation")))
                .map(DtoMapper::anomalieResume).getContent();

        return new TableauDeBordResponse(debut, fin,
                indicateur(total(actuel), total(avant)),
                indicateur(actuel, avant, StatutCode.NOUVELLE),
                indicateur(actuel, avant, StatutCode.EN_COURS),
                indicateur(actuel, avant, StatutCode.RESOLUE),
                indicateur(actuel, avant, StatutCode.FERMEE),
                evolution(org, p),
                anomalieRepository.compterParType(org, p.debut(), p.fin()),
                recentes);
    }

    @Transactional(readOnly = true)
    public StatistiquesResponse statistiques(UtilisateurConnecte courant, LocalDate debut, LocalDate fin) {
        Periode p = periode(debut, fin);
        Long org = courant.organisationId();

        List<LibelleValeur> parStatut = anomalieRepository.compterParStatut(org, p.debut(), p.fin());
        long declarees = total(parLibelle(parStatut));
        long resolues = anomalieRepository.compterResolues(org, p.debut(), p.fin());

        List<Object[]> lignesDurees = anomalieRepository.dureeMoyenneResolutionParPriorite(org, p.debut(), p.fin());
        List<DureeMoyenne> durees = lignesDurees.stream()
                .map(l -> new DureeMoyenne((String) l[0], arrondi(((Number) l[1]).doubleValue() / 60.0)))
                .toList();
        Double dureeGlobale = moyennePonderee(lignesDurees);

        List<IncidentRecurrent> recurrents = anomalieRepository.incidentsRecurrents(org, p.debut(), p.fin(), SEUIL_RECURRENCE)
                .stream().limit(10)
                .map(l -> new IncidentRecurrent((String) l[0], (String) l[1], ((Number) l[2]).longValue()))
                .toList();

        return new StatistiquesResponse(debut, fin, declarees, resolues,
                declarees == 0 ? null : arrondi(100.0 * Math.min(resolues, declarees) / declarees),
                dureeGlobale,
                trierParStatut(parStatut),
                anomalieRepository.compterParPriorite(org, p.debut(), p.fin()),
                anomalieRepository.compterParType(org, p.debut(), p.fin()),
                anomalieRepository.compterParSysteme(org, p.debut(), p.fin()),
                durees, recurrents);
    }

    // ------------------------------------------------------------------ Calculs

    private List<PointEvolution> evolution(Long org, Periode p) {
        Map<LocalDate, Long> creations = parJour(anomalieRepository.compterCreationsParJour(org, p.debut(), p.fin()));
        Map<LocalDate, Long> resolutions = parJour(anomalieRepository.compterResolutionsParJour(org, p.debut(), p.fin()));
        long nonResolues = anomalieRepository.compterNonResoluesA(org, p.debut());
        List<PointEvolution> points = new ArrayList<>();
        for (LocalDate jour = p.debut().toLocalDate(); jour.isBefore(p.fin().toLocalDate()); jour = jour.plusDays(1)) {
            long declarees = creations.getOrDefault(jour, 0L);
            long resolues = resolutions.getOrDefault(jour, 0L);
            nonResolues += declarees - resolues;
            points.add(new PointEvolution(jour, declarees, Math.max(nonResolues, 0), resolues));
        }
        return points;
    }

    /** Moyenne globale en heures, pondérée par le nombre d'anomalies résolues de chaque priorité. */
    private static Double moyennePonderee(List<Object[]> lignes) {
        double somme = 0;
        long poids = 0;
        for (Object[] l : lignes) {
            long n = ((Number) l[2]).longValue();
            somme += ((Number) l[1]).doubleValue() * n;
            poids += n;
        }
        return poids == 0 ? null : arrondi(somme / poids / 60.0);
    }

    private static List<LibelleValeur> trierParStatut(List<LibelleValeur> valeurs) {
        Map<String, Long> parNom = parLibelle(valeurs);
        return Arrays.stream(StatutCode.values())
                .map(code -> new LibelleValeur(code.getLibelle(), parNom.getOrDefault(code.getLibelle(), 0L)))
                .toList();
    }

    private static Indicateur indicateur(Map<String, Long> actuel, Map<String, Long> avant, StatutCode statut) {
        return indicateur(actuel.getOrDefault(statut.getLibelle(), 0L), avant.getOrDefault(statut.getLibelle(), 0L));
    }

    private static Indicateur indicateur(long valeur, long precedent) {
        Double variation = precedent == 0 ? null : arrondi(100.0 * (valeur - precedent) / precedent);
        return new Indicateur(valeur, variation);
    }

    private static long total(Map<String, Long> valeurs) {
        return valeurs.values().stream().mapToLong(Long::longValue).sum();
    }

    private static Map<String, Long> parLibelle(List<LibelleValeur> valeurs) {
        return valeurs.stream().collect(Collectors.toMap(LibelleValeur::libelle, LibelleValeur::valeur, Long::sum));
    }

    private static Map<LocalDate, Long> parJour(List<JourValeur> valeurs) {
        return valeurs.stream().collect(Collectors.toMap(JourValeur::jour, JourValeur::valeur, Long::sum));
    }

    private static double arrondi(double valeur) {
        return Math.round(valeur * 10) / 10.0;
    }

    static Periode periode(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) {
            throw ApiException.invalide("Les dates de début et de fin sont obligatoires.");
        }
        if (fin.isBefore(debut)) {
            throw ApiException.invalide("La date de fin doit être postérieure ou égale à la date de début.");
        }
        if (ChronoUnit.DAYS.between(debut, fin) > DUREE_MAX_JOURS) {
            throw ApiException.invalide("La période ne peut pas dépasser " + DUREE_MAX_JOURS + " jours.");
        }
        return new Periode(debut.atStartOfDay(), fin.plusDays(1).atStartOfDay());
    }

    /** Période semi-ouverte [debut, fin[. */
    record Periode(LocalDateTime debut, LocalDateTime fin) {

        Periode precedente() {
            long jours = ChronoUnit.DAYS.between(debut, fin);
            return new Periode(debut.minusDays(jours), debut);
        }
    }
}
