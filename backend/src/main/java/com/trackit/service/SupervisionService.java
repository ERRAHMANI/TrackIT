package com.trackit.service;

import com.trackit.config.TrackItProperties;
import com.trackit.dto.ChargeOperateur;
import com.trackit.dto.DelaiAnomalie;
import com.trackit.dto.SupervisionResponse;
import com.trackit.entity.Anomalie;
import com.trackit.entity.StatutCode;
import com.trackit.mapper.DtoMapper;
import com.trackit.repository.AnomalieRepository;
import com.trackit.security.UtilisateurConnecte;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Supervision des anomalies ouvertes et contrôle des délais (UC-02). */
@Service
public class SupervisionService {

    private static final List<String> STATUTS_OUVERTS =
            List.of(StatutCode.NOUVELLE.getLibelle(), StatutCode.EN_COURS.getLibelle());

    private final AnomalieRepository anomalieRepository;
    private final TrackItProperties.Sla sla;

    public SupervisionService(AnomalieRepository anomalieRepository, TrackItProperties properties) {
        this.anomalieRepository = anomalieRepository;
        this.sla = properties.sla();
    }

    @Transactional(readOnly = true)
    public SupervisionResponse superviser(UtilisateurConnecte courant) {
        Long org = courant.organisationId();
        LocalDateTime maintenant = LocalDateTime.now();

        List<DelaiAnomalie> delais = anomalieRepository.findOuvertes(org, STATUTS_OUVERTS).stream()
                .map(a -> delai(a, maintenant))
                .sorted(Comparator.comparing(DelaiAnomalie::enRetard).reversed()
                        .thenComparing(d -> d.ageHeures() - d.delaiCibleHeures(), Comparator.reverseOrder()))
                .toList();

        Map<Long, long[]> charge = new LinkedHashMap<>();
        Map<Long, String> noms = new LinkedHashMap<>();
        for (Object[] ligne : anomalieRepository.chargeParOperateur(org, STATUTS_OUVERTS)) {
            Long id = (Long) ligne[0];
            noms.put(id, ligne[1] + " " + ligne[2]);
            long[] compteurs = charge.computeIfAbsent(id, k -> new long[2]);
            compteurs[StatutCode.NOUVELLE.getLibelle().equals(ligne[3]) ? 0 : 1] += ((Number) ligne[4]).longValue();
        }
        List<ChargeOperateur> chargeOperateurs = charge.entrySet().stream()
                .map(e -> new ChargeOperateur(e.getKey(), noms.get(e.getKey()), e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingLong(ChargeOperateur::getTotal).reversed())
                .toList();

        return new SupervisionResponse(delais.size(),
                anomalieRepository.compterOuvertesNonAffectees(org, STATUTS_OUVERTS),
                delais.stream().filter(DelaiAnomalie::enRetard).count(),
                delais, chargeOperateurs);
    }

    private DelaiAnomalie delai(Anomalie a, LocalDateTime maintenant) {
        long age = Duration.between(a.getDateCreation(), maintenant).toHours();
        int cible = sla.heuresPour(a.getPriorite().getNiveau());
        return new DelaiAnomalie(DtoMapper.anomalieResume(a), age, cible, age > cible);
    }
}
