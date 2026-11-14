package com.trackit.api;

import com.trackit.entity.Anomalie;
import com.trackit.entity.RoleCode;
import com.trackit.entity.StatutCode;
import com.trackit.repository.AnomalieRepository;
import com.trackit.repository.HistoriqueRepository;
import com.trackit.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CycleDeVieAnomalieTest extends TestIntegrationBase {

    @Autowired
    private AnomalieRepository anomalieRepository;
    @Autowired
    private HistoriqueRepository historiqueRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    // ------------------------------------------------------------------ Déclaration

    @Test
    void declarationDetermineAuteurDateEtStatutNouvelle() throws Exception {
        String reponse = mvc.perform(post("/api/v1/anomalies").header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(anomalie("Erreur de connexion à l'ERP"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut.nom").value("Nouvelle"))
                .andExpect(jsonPath("$.auteur.id").value(donnees.comptes.get(RoleCode.OPERATEUR).getId()))
                .andExpect(jsonPath("$.dateCreation", notNullValue()))
                .andExpect(jsonPath("$.operateur").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Long id = json.readTree(reponse).get("id").asLong();

        assertThat(historiqueRepository.findByAnomalieIdOrderByDateModificationAscIdAsc(id))
                .singleElement().satisfies(h -> {
                    assertThat(h.getAction()).startsWith("Création");
                    assertThat(h.getNouveauStatut()).isEqualTo("Nouvelle");
                });
        // Le responsable technique est notifié de la déclaration
        assertThat(notificationRepository.countByUtilisateurIdAndLuFalse(
                donnees.comptes.get(RoleCode.RESPONSABLE_TECHNIQUE).getId())).isEqualTo(1);
    }

    @Test
    void declarationSansTitreNiPrioriteEstRefusee() throws Exception {
        Map<String, Object> invalide = anomalie("");
        invalide.remove("idPriorite");
        mvc.perform(post("/api/v1/anomalies").header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(invalide)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.titre").value("Le titre est obligatoire."))
                .andExpect(jsonPath("$.champs.idPriorite").value("La priorité est obligatoire."));
        assertThat(anomalieRepository.count()).isZero();
    }

    @Test
    void declarationSurUnSystemeDUneAutreOrganisationEstRefusee() throws Exception {
        Map<String, Object> requete = anomalie("Panne");
        requete.put("idSysteme", donnees.systemeAutreOrganisation.getId());
        mvc.perform(post("/api/v1/anomalies").header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(requete)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Système introuvable")));
    }

    // ------------------------------------------------------------------ Statuts

    @Test
    void transitionNonPrevueEstRefusee() throws Exception {
        Anomalie a = nouvelleAnomalieDeLOperateur();
        changerStatut(RoleCode.OPERATEUR, a, StatutCode.RESOLUE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Transition non autorisée")));
    }

    @Test
    void operateurQuiDemarreLeTraitementPrendLAnomalieEnCharge() throws Exception {
        Anomalie a = nouvelleAnomalieDeLOperateur();
        changerStatut(RoleCode.OPERATEUR, a, StatutCode.EN_COURS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut.nom").value("En cours"))
                .andExpect(jsonPath("$.operateur.id").value(donnees.comptes.get(RoleCode.OPERATEUR).getId()));
        assertThat(historiqueRepository.findByAnomalieIdOrderByDateModificationAscIdAsc(a.getId()))
                .extracting(h -> h.getAction())
                .containsExactly("Prise en charge par Test OPERATEUR", "Changement de statut");
    }

    @Test
    void resolutionRenseigneLaDateEtSeuleLaSupervisionPeutFermer() throws Exception {
        Anomalie a = donnees.anomalie("Lenteurs", donnees.erp, StatutCode.EN_COURS,
                donnees.comptes.get(RoleCode.OPERATEUR), donnees.comptes.get(RoleCode.OPERATEUR));

        changerStatut(RoleCode.OPERATEUR, a, StatutCode.RESOLUE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateResolution", notNullValue()));
        // Le responsable technique est prévenu qu'une anomalie résolue attend sa validation
        assertThat(notificationRepository.countByUtilisateurIdAndLuFalse(
                donnees.comptes.get(RoleCode.RESPONSABLE_TECHNIQUE).getId())).isEqualTo(1);
        changerStatut(RoleCode.OPERATEUR, a, StatutCode.FERMEE).andExpect(status().isForbidden());
        changerStatut(RoleCode.RESPONSABLE_TECHNIQUE, a, StatutCode.FERMEE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut.nom").value("Fermée"))
                .andExpect(jsonPath("$.actions.modifier").value(false));

        // Une anomalie fermée n'évolue plus
        changerStatut(RoleCode.RESPONSABLE_TECHNIQUE, a, StatutCode.EN_COURS).andExpect(status().isConflict());
        var historique = historiqueRepository.findByAnomalieIdOrderByDateModificationAscIdAsc(a.getId());
        assertThat(historique).extracting(h -> h.getAncienStatut() + "→" + h.getNouveauStatut())
                .containsExactly("En cours→Résolue", "Résolue→Fermée");
    }

    @Test
    void reouvertureEffaceLaDateDeResolution() throws Exception {
        Anomalie a = donnees.anomalie("Wi-Fi instable", donnees.reseau, StatutCode.RESOLUE,
                donnees.comptes.get(RoleCode.OPERATEUR), donnees.comptes.get(RoleCode.OPERATEUR));
        changerStatut(RoleCode.OPERATEUR, a, StatutCode.EN_COURS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateResolution").doesNotExist());
        assertThat(historiqueRepository.findByAnomalieIdOrderByDateModificationAscIdAsc(a.getId()))
                .singleElement().satisfies(h -> assertThat(h.getAction()).isEqualTo("Réouverture de l'anomalie"));
    }

    @Test
    void operateurNonConcerneNePeutPasModifierLAnomalieDUnCollegue() throws Exception {
        Anomalie a = donnees.anomalie("Imprimante", donnees.erp, StatutCode.EN_COURS, donnees.operateur2, donnees.operateur2);
        changerStatut(RoleCode.OPERATEUR, a, StatutCode.RESOLUE).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/anomalies/" + a.getId()).header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(anomalie("Nouveau titre"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void modificationDePrioriteEstTraceeAvecAncienneEtNouvelleValeur() throws Exception {
        Anomalie a = nouvelleAnomalieDeLOperateur();
        mvc.perform(patch("/api/v1/anomalies/" + a.getId() + "/priorite").header("Authorization", donnees.jeton(RoleCode.RESPONSABLE_TECHNIQUE))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("idPriorite", donnees.critique.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priorite.nom").value("Critique"));
        assertThat(historiqueRepository.findByAnomalieIdOrderByDateModificationAscIdAsc(a.getId()))
                .singleElement().satisfies(h -> {
                    assertThat(h.getAnciennePriorite()).isEqualTo("Basse");
                    assertThat(h.getNouvellePriorite()).isEqualTo("Critique");
                });
    }

    // ------------------------------------------------------------------ Affectation

    @Test
    void responsableAffecteEtReaffecte() throws Exception {
        Anomalie a = nouvelleAnomalieDeLOperateur();
        affecter(RoleCode.RESPONSABLE_TECHNIQUE, a, donnees.operateur2.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operateur.id").value(donnees.operateur2.getId()))
                .andExpect(jsonPath("$.statut.nom").value("Nouvelle"));
        affecter(RoleCode.RESPONSABLE_TECHNIQUE, a, donnees.comptes.get(RoleCode.OPERATEUR).getId())
                .andExpect(status().isOk());
        assertThat(historiqueRepository.findByAnomalieIdOrderByDateModificationAscIdAsc(a.getId()))
                .extracting(h -> h.getAction())
                .containsExactly("Affectation à Test OPERATEUR2 (Opérateur)", "Réaffectation : Test OPERATEUR2 → Test OPERATEUR");
        assertThat(notificationRepository.countByUtilisateurIdAndLuFalse(donnees.operateur2.getId())).isEqualTo(2);
    }

    @Test
    void declarantReaffecteAUnCollegueRecoitUneSeuleNotification() throws Exception {
        var declarant = donnees.comptes.get(RoleCode.OPERATEUR);
        Anomalie a = donnees.anomalie("Imprimante bloquée", donnees.erp, StatutCode.NOUVELLE, declarant, declarant);

        affecter(RoleCode.RESPONSABLE_TECHNIQUE, a, donnees.operateur2.getId()).andExpect(status().isOk());

        // « a été réaffectée à … » suffit : pas de second message « prise en charge par … »
        assertThat(notificationRepository.countByUtilisateurIdAndLuFalse(declarant.getId())).isEqualTo(1);
        assertThat(notificationRepository.countByUtilisateurIdAndLuFalse(donnees.operateur2.getId())).isEqualTo(1);
    }

    @Test
    void affectationAUnNonOperateurOuAUneAutreOrganisationEstRefusee() throws Exception {
        Anomalie a = nouvelleAnomalieDeLOperateur();
        affecter(RoleCode.RESPONSABLE_TECHNIQUE, a, donnees.comptes.get(RoleCode.ANALYSTE).getId()).andExpect(status().isBadRequest());
        affecter(RoleCode.RESPONSABLE_TECHNIQUE, a, donnees.operateurAutreOrganisation.getId()).andExpect(status().isBadRequest());
    }

    @Test
    void operateurPeutSeulementSeLAffecterSiElleEstLibre() throws Exception {
        Anomalie a = donnees.anomalie("VPN", donnees.reseau, StatutCode.NOUVELLE, donnees.operateur2, null);
        affecter(RoleCode.OPERATEUR, a, donnees.operateur2.getId()).andExpect(status().isForbidden());
        affecter(RoleCode.OPERATEUR, a, donnees.comptes.get(RoleCode.OPERATEUR).getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operateur.id").value(donnees.comptes.get(RoleCode.OPERATEUR).getId()));
        // Déjà affectée : un autre opérateur ne peut plus la prendre
        mvc.perform(patch("/api/v1/anomalies/" + a.getId() + "/affectation").header("Authorization", donnees.jeton(donnees.operateur2))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("idOperateur", donnees.operateur2.getId()))))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ Outils

    private Anomalie nouvelleAnomalieDeLOperateur() {
        return donnees.anomalie("Erreur de synchronisation", donnees.erp, StatutCode.NOUVELLE,
                donnees.comptes.get(RoleCode.OPERATEUR), null);
    }

    private ResultActions changerStatut(RoleCode role, Anomalie a, StatutCode cible) throws Exception {
        return mvc.perform(patch("/api/v1/anomalies/" + a.getId() + "/statut").header("Authorization", donnees.jeton(role))
                .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("idStatut", donnees.statuts.get(cible).getId()))));
    }

    private ResultActions affecter(RoleCode role, Anomalie a, Long idOperateur) throws Exception {
        return mvc.perform(patch("/api/v1/anomalies/" + a.getId() + "/affectation").header("Authorization", donnees.jeton(role))
                .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("idOperateur", idOperateur))));
    }

    private Map<String, Object> anomalie(String titre) {
        Map<String, Object> m = new HashMap<>();
        m.put("titre", titre);
        m.put("description", "L'application est inaccessible depuis ce matin.");
        m.put("idSysteme", donnees.erp.getId());
        m.put("idType", donnees.typeApplication.getId());
        m.put("idPriorite", donnees.basse.getId());
        return m;
    }
}
