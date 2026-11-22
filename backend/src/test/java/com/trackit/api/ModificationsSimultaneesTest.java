package com.trackit.api;

import com.trackit.entity.Anomalie;
import com.trackit.entity.RoleCode;
import com.trackit.entity.StatutCode;
import com.trackit.entity.Utilisateur;
import com.trackit.exception.ApiException;
import com.trackit.repository.AnomalieRepository;
import com.trackit.service.EscaladeDelaisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deux techniciens qui modifient la même anomalie en même temps (verrou optimiste, migration V9) :
 * la seconde écriture est refusée par un 409 au lieu d'écraser la première.
 */
class ModificationsSimultaneesTest extends TestIntegrationBase {

    @Autowired
    private AnomalieRepository anomalieRepository;
    @Autowired
    private EscaladeDelaisService escaladeDelaisService;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void laVersionEstRenvoyeeEtAugmenteAChaqueModification() throws Exception {
        Anomalie a = anomalie();
        long avant = version(RoleCode.OPERATEUR, a);

        mvc.perform(patch("/api/v1/anomalies/" + a.getId() + "/priorite").header("Authorization", donnees.jeton(RoleCode.RESPONSABLE_TECHNIQUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("idPriorite", donnees.critique.getId(), "version", avant))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(avant + 1));
        assertThat(version(RoleCode.OPERATEUR, a)).isEqualTo(avant + 1);
    }

    @Test
    void uneFicheDevenueObsoleteNEcrasePasLaModificationDUnCollegue() throws Exception {
        Anomalie a = anomalie();
        // L'opérateur ouvre le formulaire de modification…
        long ficheDeLOperateur = version(RoleCode.OPERATEUR, a);

        // … pendant que le responsable technique passe l'anomalie en priorité critique.
        mvc.perform(patch("/api/v1/anomalies/" + a.getId() + "/priorite").header("Authorization", donnees.jeton(RoleCode.RESPONSABLE_TECHNIQUE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("idPriorite", donnees.critique.getId(), "version", ficheDeLOperateur))))
                .andExpect(status().isOk());

        // L'opérateur enregistre son formulaire, qui contient encore l'ancienne priorité : refusé.
        mvc.perform(put("/api/v1/anomalies/" + a.getId()).header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(formulaire(a, ficheDeLOperateur))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ApiException.MODIFIEE_ENTRE_TEMPS));

        // Rien n'a été écrasé : la priorité du responsable est conservée, le titre n'a pas changé.
        mvc.perform(get("/api/v1/anomalies/" + a.getId()).header("Authorization", donnees.jeton(RoleCode.OPERATEUR)))
                .andExpect(jsonPath("$.priorite.nom").value(donnees.critique.getNom()))
                .andExpect(jsonPath("$.titre").value("Erreur de synchronisation"));

        // Après rechargement de la fiche, la modification passe.
        Map<String, Object> recharge = formulaire(a, version(RoleCode.OPERATEUR, a));
        recharge.put("idPriorite", donnees.critique.getId());
        mvc.perform(put("/api/v1/anomalies/" + a.getId()).header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(recharge)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Erreur de synchronisation ERP"));
    }

    @Test
    void uneEcritureConcurrenteEstBloqueeMemeSansVersionDansLaRequete() throws Exception {
        Anomalie a = anomalie();
        // Un collègue enregistre une modification juste après que la requête a lu l'anomalie : on le
        // simule en changeant la ligne directement en base ; l'anomalie déjà chargée devient obsolète.
        jdbc.update("UPDATE anomalie SET titre = 'Titre du collègue', version = version + 1 WHERE id_anomalie = ?", a.getId());

        Map<String, Object> sansVersion = formulaire(a, null);
        sansVersion.remove("version");
        mvc.perform(put("/api/v1/anomalies/" + a.getId()).header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(sansVersion)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ApiException.MODIFIEE_ENTRE_TEMPS));
        assertThat(jdbc.queryForObject("SELECT titre FROM anomalie WHERE id_anomalie = ?", String.class, a.getId()))
                .isEqualTo("Titre du collègue");
    }

    @Test
    void lEscaladeAutomatiqueNInvalidePasLaFicheOuverte() {
        Anomalie a = anomalie();
        a.setPriorite(donnees.critique);
        a.setDateCreation(LocalDateTime.now().minusDays(3));
        anomalieRepository.saveAndFlush(a);
        Long avant = jdbc.queryForObject("SELECT version FROM anomalie WHERE id_anomalie = ?", Long.class, a.getId());

        assertThat(escaladeDelaisService.escalader(LocalDateTime.now())).isEqualTo(1);
        anomalieRepository.flush();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM anomalie WHERE id_anomalie = ? AND date_escalade IS NOT NULL",
                Long.class, a.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version FROM anomalie WHERE id_anomalie = ?", Long.class, a.getId())).isEqualTo(avant);
    }

    // ------------------------------------------------------------------ Outils

    private Anomalie anomalie() {
        // En cours et affectée à l'opérateur : il peut la modifier, le responsable technique aussi.
        Utilisateur operateur = donnees.comptes.get(RoleCode.OPERATEUR);
        return donnees.anomalie("Erreur de synchronisation", donnees.erp, StatutCode.EN_COURS, operateur, operateur);
    }

    private long version(RoleCode role, Anomalie a) throws Exception {
        String reponse = mvc.perform(get("/api/v1/anomalies/" + a.getId()).header("Authorization", donnees.jeton(role)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(reponse).get("version").asLong();
    }

    /** Le formulaire de modification tel que l'opérateur l'a ouvert (priorité basse), titre complété. */
    private Map<String, Object> formulaire(Anomalie a, Long version) {
        Map<String, Object> m = new HashMap<>();
        m.put("titre", "Erreur de synchronisation ERP");
        m.put("description", a.getDescription());
        m.put("idSysteme", a.getSysteme().getId());
        m.put("idType", a.getType().getId());
        m.put("idPriorite", donnees.basse.getId());
        m.put("version", version);
        return m;
    }
}
