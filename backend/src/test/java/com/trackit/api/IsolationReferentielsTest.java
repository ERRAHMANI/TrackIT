package com.trackit.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.trackit.entity.RoleCode;
import com.trackit.entity.StatutCode;
import com.trackit.repository.PrioriteRepository;
import com.trackit.repository.StatutRepository;
import com.trackit.repository.TypeAnomalieRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Types d'anomalie et priorités propres à chaque organisation (OWASP A01, action P7) : un client ne
 * peut ni voir, ni modifier, ni supprimer, ni utiliser les valeurs d'un autre client.
 */
class IsolationReferentielsTest extends TestIntegrationBase {

    @Autowired
    private TypeAnomalieRepository typeRepository;
    @Autowired
    private PrioriteRepository prioriteRepository;
    @Autowired
    private StatutRepository statutRepository;

    // ------------------------------------------------------------------ Lecture

    @Test
    void lesListesNeContiennentQueLesValeursDeLOrganisation() throws Exception {
        String adminA = donnees.jeton(RoleCode.ADMINISTRATEUR);
        mvc.perform(get("/api/v1/types").header("Authorization", adminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", not(hasItem(donnees.typeReseauAutreOrganisation.getId().intValue()))));
        mvc.perform(get("/api/v1/priorites").header("Authorization", adminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].id", not(hasItem(donnees.basseAutreOrganisation.getId().intValue()))));
    }

    // ------------------------------------------------------------------ Modification et suppression

    @Test
    void unAdministrateurNePeutPasModifierUnTypeDUneAutreOrganisation() throws Exception {
        Long idTypeB = donnees.typeReseauAutreOrganisation.getId();
        mvc.perform(put("/api/v1/types/" + idTypeB).header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("nom", "Piraté"))))
                .andExpect(status().isNotFound());
        assertThat(typeRepository.findById(idTypeB)).get().extracting("nom").isEqualTo("Réseau");
    }

    @Test
    void unAdministrateurNePeutPasSupprimerUnTypeDUneAutreOrganisation() throws Exception {
        Long idTypeB = donnees.typeReseauAutreOrganisation.getId();
        mvc.perform(delete("/api/v1/types/" + idTypeB).header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
        assertThat(typeRepository.existsById(idTypeB)).isTrue();
    }

    @Test
    void unAdministrateurNePeutPasModifierUnePrioriteDUneAutreOrganisation() throws Exception {
        Long idPrioriteB = donnees.basseAutreOrganisation.getId();
        mvc.perform(put("/api/v1/priorites/" + idPrioriteB).header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("nom", "Piratée", "niveau", 4))))
                .andExpect(status().isNotFound());
        assertThat(prioriteRepository.findById(idPrioriteB)).get().extracting("nom").isEqualTo("Basse");
    }

    @Test
    void unAdministrateurNePeutPasSupprimerUnePrioriteDUneAutreOrganisation() throws Exception {
        Long idPrioriteB = donnees.basseAutreOrganisation.getId();
        mvc.perform(delete("/api/v1/priorites/" + idPrioriteB).header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
        assertThat(prioriteRepository.existsById(idPrioriteB)).isTrue();
    }

    @Test
    void unNomDejaUtiliseParUneAutreOrganisationResteDisponible() throws Exception {
        // « Réseau » existe dans A et dans B : chaque organisation gère ses propres noms.
        mvc.perform(post("/api/v1/types").header("Authorization", donnees.jeton(donnees.administrateurAutreOrganisation))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("nom", "Application"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/types").header("Authorization", donnees.jeton(donnees.administrateurAutreOrganisation))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("nom", "Réseau"))))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ Statuts communs

    @Test
    void lesStatutsCommunsNeSontPlusModifiablesParUnClient() throws Exception {
        Long idNouvelle = donnees.statuts.get(StatutCode.NOUVELLE).getId();
        mvc.perform(put("/api/v1/statuts/" + idNouvelle).header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("description", "Modifiée par un client"))))
                .andExpect(status().is4xxClientError());
        assertThat(statutRepository.findById(idNouvelle)).get().extracting("description").isNotEqualTo("Modifiée par un client");
    }

    // ------------------------------------------------------------------ Utilisation dans une anomalie

    @Test
    void uneAnomalieNePeutPasEtreDeclareeAvecUnePrioriteDUneAutreOrganisation() throws Exception {
        Map<String, Object> requete = anomalie();
        requete.put("idPriorite", donnees.basseAutreOrganisation.getId());
        mvc.perform(post("/api/v1/anomalies").header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(requete)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Priorité inconnue")));
    }

    @Test
    void uneAnomalieNePeutPasEtreDeclareeAvecUnTypeDUneAutreOrganisation() throws Exception {
        Map<String, Object> requete = anomalie();
        requete.put("idType", donnees.typeReseauAutreOrganisation.getId());
        mvc.perform(post("/api/v1/anomalies").header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(requete)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Type d'anomalie inconnu")));
    }

    @Test
    void uneAnomalieExistanteNePeutPasRecevoirUnePrioriteDUneAutreOrganisation() throws Exception {
        var a = donnees.anomalie("Lenteurs", donnees.erp, StatutCode.NOUVELLE, donnees.comptes.get(RoleCode.OPERATEUR), null);
        Map<String, Object> requete = anomalie();
        requete.put("idPriorite", donnees.basseAutreOrganisation.getId());
        requete.put("idType", donnees.typeReseau.getId());
        mvc.perform(put("/api/v1/anomalies/" + a.getId()).header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(requete)))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/anomalies/" + a.getId() + "/priorite").header("Authorization", donnees.jeton(RoleCode.RESPONSABLE_TECHNIQUE))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("idPriorite", donnees.basseAutreOrganisation.getId()))))
                .andExpect(status().isBadRequest());
        assertThat(prioriteRepository.findById(donnees.basse.getId())).isPresent();
        mvc.perform(get("/api/v1/anomalies/" + a.getId()).header("Authorization", donnees.jeton(RoleCode.OPERATEUR)))
                .andExpect(jsonPath("$.priorite.id").value(donnees.basse.getId().intValue()));
    }

    // ------------------------------------------------------------------ Inscription

    @Test
    void uneNouvelleInscriptionRecoitSesPropresReferentielsParDefaut() throws Exception {
        String reponse = mvc.perform(post("/api/v1/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("nomOrganisation", "Atlas Logistique", "emailOrganisation", "contact@atlas.test",
                                "nom", "Bennani", "prenom", "Salma", "email", "salma@atlas.test", "motDePasse", "Ciel-Bleu-Montagne-7",
                                "accepteConditions", true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode corpsReponse = json.readTree(reponse);
        String jeton = "Bearer " + corpsReponse.get("jeton").asText();

        mvc.perform(get("/api/v1/types").header("Authorization", jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[*].nom", hasItems("Réseau", "Application", "Matériel", "Accès",
                        "Base de données", "Performance", "Fonctionnel", "Autre")))
                .andExpect(jsonPath("$[*].id", not(hasItem(donnees.typeReseau.getId().intValue()))));
        mvc.perform(get("/api/v1/priorites").header("Authorization", jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nom", contains("Basse", "Moyenne", "Haute", "Critique")))
                .andExpect(jsonPath("$[*].niveau", contains(1, 2, 3, 4)))
                .andExpect(jsonPath("$[*].id", not(hasItem(donnees.basse.getId().intValue()))));
    }

    private Map<String, Object> anomalie() {
        Map<String, Object> m = new HashMap<>();
        m.put("titre", "Panne de l'ERP");
        m.put("description", "L'application est inaccessible depuis ce matin.");
        m.put("idSysteme", donnees.erp.getId());
        m.put("idType", donnees.typeApplication.getId());
        m.put("idPriorite", donnees.basse.getId());
        return m;
    }
}
