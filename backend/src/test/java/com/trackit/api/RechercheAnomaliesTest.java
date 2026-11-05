package com.trackit.api;

import com.trackit.entity.RoleCode;
import com.trackit.entity.StatutCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RechercheAnomaliesTest extends TestIntegrationBase {

    private Long idVpn;

    @BeforeEach
    void creerAnomalies() {
        var op = donnees.comptes.get(RoleCode.OPERATEUR);
        idVpn = donnees.anomalie("Problème de connexion VPN", donnees.reseau, StatutCode.EN_COURS, op, op).getId();
        donnees.anomalie("Coupure réseau bâtiment A", donnees.reseau, StatutCode.EN_COURS, op, donnees.operateur2);
        donnees.anomalie("Application ERP indisponible", donnees.erp, StatutCode.NOUVELLE, donnees.operateur2, null);
        donnees.anomalie("Lenteurs ERP", donnees.erp, StatutCode.FERMEE, op, op);
        donnees.anomalie("Réseau indisponible chez B", donnees.systemeAutreOrganisation, StatutCode.EN_COURS,
                donnees.operateurAutreOrganisation, null);
    }

    @Test
    void neRetourneQueLesAnomaliesDeLOrganisation() throws Exception {
        mvc.perform(get("/api/v1/anomalies").header("Authorization", donnees.jeton(RoleCode.UTILISATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void filtresTexteEtStatutSontCumules() throws Exception {
        mvc.perform(get("/api/v1/anomalies").param("q", "réseau").param("idStatut", donnees.statuts.get(StatutCode.EN_COURS).getId().toString())
                        .header("Authorization", donnees.jeton(RoleCode.UTILISATEUR)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.contenu[*].titre", containsInAnyOrder("Problème de connexion VPN", "Coupure réseau bâtiment A")));
    }

    @Test
    void rechercheParNumero() throws Exception {
        mvc.perform(get("/api/v1/anomalies").param("q", String.format("#%03d", idVpn))
                        .header("Authorization", donnees.jeton(RoleCode.UTILISATEUR)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.contenu[0].id").value(idVpn));
    }

    @Test
    void filtresSystemeOperateurEtNonAffectee() throws Exception {
        String jeton = donnees.jeton(RoleCode.RESPONSABLE_TECHNIQUE);
        mvc.perform(get("/api/v1/anomalies").param("idSysteme", donnees.erp.getId().toString()).header("Authorization", jeton))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/v1/anomalies").param("idOperateur", donnees.operateur2.getId().toString()).header("Authorization", jeton))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/anomalies").param("nonAffectee", "true").header("Authorization", jeton))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void mesTachesRetourneLesAnomaliesAffecteesALOperateur() throws Exception {
        mvc.perform(get("/api/v1/anomalies").param("affecteesAMoi", "true").header("Authorization", donnees.jeton(RoleCode.OPERATEUR)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void paginationEtTri() throws Exception {
        mvc.perform(get("/api/v1/anomalies").param("taille", "3").param("page", "1").param("tri", "titre").param("sens", "asc")
                        .header("Authorization", donnees.jeton(RoleCode.UTILISATEUR)))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.contenu.length()").value(1))
                .andExpect(jsonPath("$.contenu[0].titre").value("Problème de connexion VPN"));
    }

    @Test
    void parametreDeDateInvalideRetourne400() throws Exception {
        mvc.perform(get("/api/v1/anomalies").param("dateDebut", "05/10/2026").header("Authorization", donnees.jeton(RoleCode.UTILISATEUR)))
                .andExpect(status().isBadRequest());
    }
}
