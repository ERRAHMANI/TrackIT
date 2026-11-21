package com.trackit.service;

import com.trackit.config.TrackItProperties;
import com.trackit.entity.Anomalie;
import com.trackit.entity.RoleCode;
import com.trackit.entity.StatutCode;
import com.trackit.entity.Utilisateur;
import com.trackit.repository.AnomalieRepository;
import com.trackit.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.trackit.mapper.DtoMapper.reference;

/**
 * Escalade automatique des délais dépassés (UC-02 Supervision). Une anomalie Nouvelle ou En cours
 * dont l'âge dépasse le délai cible de sa priorité (même règle que l'écran Supervision) est signalée
 * aux responsables techniques actifs de son organisation et à l'opérateur affecté, une seule fois
 * (date_escalade). Le cycle de vie n'est pas modifié : aucun statut n'est ajouté.
 */
@Service
public class EscaladeDelaisService {

    private static final List<String> STATUTS_OUVERTS =
            List.of(StatutCode.NOUVELLE.getLibelle(), StatutCode.EN_COURS.getLibelle());

    private final AnomalieRepository anomalieRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final TrackItProperties.Sla sla;

    public EscaladeDelaisService(AnomalieRepository anomalieRepository, UtilisateurRepository utilisateurRepository,
                                 NotificationService notificationService, TrackItProperties properties) {
        this.anomalieRepository = anomalieRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.notificationService = notificationService;
        this.sla = properties.sla();
    }

    /** Escalade les anomalies en retard à l'instant donné ; retourne le nombre d'anomalies escaladées. */
    @Transactional
    public int escalader(LocalDateTime maintenant) {
        int escaladees = 0;
        for (Anomalie a : anomalieRepository.findOuvertesNonEscaladees(STATUTS_OUVERTS)) {
            int niveau = a.getPriorite().getNiveau();
            if (!sla.estDepasse(a.getDateCreation(), niveau, maintenant)) {
                continue;
            }
            a.setDateEscalade(maintenant);
            String message = "Délai dépassé : l'anomalie " + reference(a.getId()) + " « " + a.getTitre()
                    + " » est ouverte depuis " + TrackItProperties.Sla.ageHeures(a.getDateCreation(), maintenant)
                    + " h, pour un délai cible de " + sla.heuresPour(niveau) + " h.";
            destinataires(a).values().forEach(u -> notificationService.creer(u, a, message));
            escaladees++;
        }
        return escaladees;
    }

    /** Responsables techniques actifs de l'organisation de l'anomalie, plus l'opérateur affecté (sans doublon). */
    private Map<Long, Utilisateur> destinataires(Anomalie a) {
        Map<Long, Utilisateur> destinataires = new LinkedHashMap<>();
        utilisateurRepository.findByOrganisationIdAndRoleNomAndActifTrueOrderByNomAscPrenomAsc(
                        a.getSysteme().getOrganisation().getId(), RoleCode.RESPONSABLE_TECHNIQUE.getLibelle())
                .forEach(u -> destinataires.put(u.getId(), u));
        Utilisateur operateur = a.getOperateur();
        if (operateur != null && operateur.isActif()) {
            destinataires.putIfAbsent(operateur.getId(), operateur);
        }
        return destinataires;
    }
}
