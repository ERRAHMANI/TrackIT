package com.trackit.api;

import com.trackit.entity.Anomalie;
import com.trackit.entity.RoleCode;
import com.trackit.entity.StatutCode;
import com.trackit.entity.Utilisateur;
import com.trackit.repository.AnomalieRepository;
import com.trackit.repository.NotificationRepository;
import com.trackit.service.EscaladeDelaisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Escalade automatique des délais dépassés : même règle que la supervision (âge en heures supérieur
 * au délai cible de la priorité), notification unique au responsable technique et à l'opérateur
 * affecté, jamais pour une anomalie résolue ou fermée, jamais vers une autre organisation.
 */
class EscaladeDelaisTest extends TestIntegrationBase {

    @Autowired
    private EscaladeDelaisService escaladeDelaisService;
    @Autowired
    private AnomalieRepository anomalieRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private final LocalDateTime maintenant = LocalDateTime.now();

    @Test
    void uneAnomalieCritiqueHorsDelaiEstSignaleeUneSeuleFois() {
        Utilisateur operateur = donnees.comptes.get(RoleCode.OPERATEUR);
        Anomalie a = anomalie("Serveur de paie arrêté", StatutCode.EN_COURS, operateur, 5); // critique : 4 h

        assertThat(escaladeDelaisService.escalader(maintenant)).isEqualTo(1);
        assertThat(nonLues(donnees.comptes.get(RoleCode.RESPONSABLE_TECHNIQUE))).isEqualTo(1);
        assertThat(nonLues(operateur)).isEqualTo(1);
        assertThat(anomalieRepository.findById(a.getId())).get().extracting(Anomalie::getDateEscalade).isEqualTo(maintenant);

        // Deuxième passage : déjà escaladée, aucune nouvelle notification.
        assertThat(escaladeDelaisService.escalader(maintenant.plusMinutes(15))).isZero();
        assertThat(nonLues(donnees.comptes.get(RoleCode.RESPONSABLE_TECHNIQUE))).isEqualTo(1);
    }

    @Test
    void uneAnomalieDansLesDelaisNEstPasSignalee() {
        anomalie("Lenteur ponctuelle", StatutCode.NOUVELLE, null, 3); // critique : 3 h < 4 h
        assertThat(escaladeDelaisService.escalader(maintenant)).isZero();
        assertThat(nonLues(donnees.comptes.get(RoleCode.RESPONSABLE_TECHNIQUE))).isZero();
    }

    @Test
    void uneAnomalieResolueOuFermeeNEstPasSignalee() {
        anomalie("Déjà résolue", StatutCode.RESOLUE, null, 48);
        anomalie("Déjà fermée", StatutCode.FERMEE, null, 48);
        assertThat(escaladeDelaisService.escalader(maintenant)).isZero();
    }

    @Test
    void leResponsableDUneAutreOrganisationNEstPasPrevenu() {
        Anomalie autre = donnees.anomalie("Panne chez un autre client", donnees.systemeAutreOrganisation,
                StatutCode.NOUVELLE, donnees.operateurAutreOrganisation, null);
        autre.setPriorite(donnees.basseAutreOrganisation);
        autre.setDateCreation(maintenant.minusDays(30)); // basse : 168 h
        anomalieRepository.flush();

        assertThat(escaladeDelaisService.escalader(maintenant)).isEqualTo(1);
        assertThat(nonLues(donnees.comptes.get(RoleCode.RESPONSABLE_TECHNIQUE))).isZero();
    }

    private Anomalie anomalie(String titre, StatutCode statut, Utilisateur operateur, int ageHeures) {
        Anomalie a = donnees.anomalie(titre, donnees.erp, statut, donnees.comptes.get(RoleCode.OPERATEUR), operateur);
        a.setPriorite(donnees.critique);
        a.setDateCreation(maintenant.minusHours(ageHeures).minusMinutes(1));
        anomalieRepository.flush();
        return a;
    }

    private long nonLues(Utilisateur utilisateur) {
        return notificationRepository.countByUtilisateurIdAndLuFalse(utilisateur.getId());
    }
}
