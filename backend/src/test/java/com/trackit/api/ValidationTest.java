package com.trackit.api;

import com.trackit.entity.RoleCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validation côté serveur : le navigateur vérifie déjà les formulaires, mais l'API ne lui fait pas
 * confiance. Une requête envoyée directement (Postman, Swagger, script) est refusée avec un 400 et un
 * message par champ.
 */
class ValidationTest extends TestIntegrationBase {

    @Test
    void anomalieSansTitreNiDescriptionEstRefusee() throws Exception {
        Map<String, Object> corps = new HashMap<>();
        corps.put("titre", " ");
        corps.put("description", "");
        mvc.perform(post("/api/v1/anomalies").header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(corps)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.titre", notNullValue()))
                .andExpect(jsonPath("$.champs.description", notNullValue()));
    }

    @Test
    void adresseEmailInvalideEstRefusee() throws Exception {
        mvc.perform(post("/api/v1/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("nomOrganisation", "Atlas Logistique", "emailOrganisation", "contact@atlas.test",
                                "nom", "Bennani", "prenom", "Salma", "email", "pas-une-adresse",
                                "motDePasse", "Ciel-Bleu-Montagne-7"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.email", containsString("n'est pas valide")));
    }

    @Test
    void descriptionDeSystemeTropLongueEstRefusee() throws Exception {
        mvc.perform(post("/api/v1/systemes").header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("nom", "Serveur de test", "description", "x".repeat(2001), "actif", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.description", containsString("2 000 caractères")));
    }

    @Test
    void periodeDeRapportInverseeEstRefusee() throws Exception {
        mvc.perform(post("/api/v1/rapports").header("Authorization", donnees.jeton(RoleCode.ANALYSTE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("titre", "Bilan", "type", "SYNTHESE", "format", "PDF",
                                "dateDebut", "2026-10-31", "dateFin", "2026-10-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("champ")));
    }

    @Test
    void corpsJsonIllisibleEstRefuseSansDetailTechnique() throws Exception {
        mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON).content("{ pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
