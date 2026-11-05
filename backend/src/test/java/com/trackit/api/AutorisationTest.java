package com.trackit.api;

import com.trackit.entity.RoleCode;
import com.trackit.entity.StatutCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutorisationTest extends TestIntegrationBase {

    @ParameterizedTest
    @EnumSource(value = RoleCode.class, names = {"UTILISATEUR", "ANALYSTE", "ADMINISTRATEUR", "RESPONSABLE_TECHNIQUE"})
    void seulOperateurPeutDeclarer(RoleCode role) throws Exception {
        mvc.perform(post("/api/v1/anomalies").header("Authorization", donnees.jeton(role))
                        .contentType(MediaType.APPLICATION_JSON).content(anomalieValide()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(RoleCode.class)
    void tousLesRolesPeuventConsulterLesAnomalies(RoleCode role) throws Exception {
        mvc.perform(get("/api/v1/anomalies").header("Authorization", donnees.jeton(role))).andExpect(status().isOk());
    }

    @Test
    void gestionDesComptesReserveeALAdministrateur() throws Exception {
        mvc.perform(get("/api/v1/utilisateurs").header("Authorization", donnees.jeton(RoleCode.OPERATEUR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/utilisateurs").header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR)))
                .andExpect(status().isOk());
    }

    @Test
    void statistiquesReserveesAAnalysteEtResponsable() throws Exception {
        String url = "/api/v1/statistiques?debut=2026-01-01&fin=2026-12-31";
        mvc.perform(get(url).header("Authorization", donnees.jeton(RoleCode.UTILISATEUR))).andExpect(status().isForbidden());
        mvc.perform(get(url).header("Authorization", donnees.jeton(RoleCode.ANALYSTE))).andExpect(status().isOk());
        mvc.perform(get(url).header("Authorization", donnees.jeton(RoleCode.RESPONSABLE_TECHNIQUE))).andExpect(status().isOk());
    }

    @Test
    void supervisionReserveeAuResponsableTechnique() throws Exception {
        mvc.perform(get("/api/v1/supervision").header("Authorization", donnees.jeton(RoleCode.OPERATEUR))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/supervision").header("Authorization", donnees.jeton(RoleCode.RESPONSABLE_TECHNIQUE))).andExpect(status().isOk());
    }

    @Test
    void rapportsReservesALAnalyste() throws Exception {
        mvc.perform(get("/api/v1/rapports").header("Authorization", donnees.jeton(RoleCode.OPERATEUR))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/rapports").header("Authorization", donnees.jeton(RoleCode.ANALYSTE))).andExpect(status().isOk());
    }

    @Test
    void generationDeRapportExigeUnAbonnementPremium() throws Exception {
        mvc.perform(post("/api/v1/rapports").header("Authorization", donnees.jeton(RoleCode.ANALYSTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("titre", "Bilan", "type", "SYNTHESE", "format", "PDF",
                                "dateDebut", "2026-09-01", "dateFin", "2026-09-30"))))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void anomalieDUneAutreOrganisationEstInvisible() throws Exception {
        var a = donnees.anomalie("Panne ERP B", donnees.systemeAutreOrganisation, StatutCode.NOUVELLE,
                donnees.operateurAutreOrganisation, null);
        mvc.perform(get("/api/v1/anomalies/" + a.getId()).header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/anomalies/" + a.getId()).header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void utilisateurNePeutPasCommenter() throws Exception {
        var a = donnees.anomalie("Lenteurs", donnees.erp, StatutCode.NOUVELLE, donnees.comptes.get(RoleCode.OPERATEUR), null);
        mvc.perform(post("/api/v1/anomalies/" + a.getId() + "/commentaires").header("Authorization", donnees.jeton(RoleCode.UTILISATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contenu\":\"Bonjour\"}"))
                .andExpect(status().isForbidden());
    }

    private String anomalieValide() throws Exception {
        return corps(Map.of("titre", "Panne", "description", "Le service ne répond plus", "idSysteme", donnees.erp.getId(),
                "idType", donnees.typeReseau.getId(), "idPriorite", donnees.basse.getId()));
    }
}
