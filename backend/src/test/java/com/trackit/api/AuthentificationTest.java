package com.trackit.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthentificationTest extends TestIntegrationBase {

    @Test
    void connexionValideRetourneUnJetonEtLeRole() throws Exception {
        mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", "operateur@org-a.test", "motDePasse", DonneesTest.MOT_DE_PASSE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton", not(emptyString())))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.utilisateur.codeRole").value("OPERATEUR"))
                .andExpect(jsonPath("$.utilisateur.motDePasse").doesNotExist());
    }

    @Test
    void motDePasseIncorrectEstRefuseAvec401() throws Exception {
        mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", "operateur@org-a.test", "motDePasse", "mauvais"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Identifiants incorrects")));
    }

    @Test
    void compteDesactiveEstRefuse() throws Exception {
        mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", "inactif@org-a.test", "motDePasse", DonneesTest.MOT_DE_PASSE))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("désactivé")));
    }

    @Test
    void champsManquantsRetournent400AvecLeDetailParChamp() throws Exception {
        mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"pas-un-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.email").exists())
                .andExpect(jsonPath("$.champs.motDePasse").exists());
    }

    @Test
    void routeProtegeeSansJetonRetourne401() throws Exception {
        mvc.perform(get("/api/v1/anomalies")).andExpect(status().isUnauthorized());
    }

    @Test
    void jetonFalsifieRetourne401() throws Exception {
        mvc.perform(get("/api/v1/anomalies").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.e30.signature-invalide"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inscriptionCreeOrganisationEtAdministrateurAvecMotDePasseHache() throws Exception {
        mvc.perform(post("/api/v1/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("nomOrganisation", "Atlas Logistique", "emailOrganisation", "contact@atlas.test",
                                "nom", "Bennani", "prenom", "Salma", "email", "salma@atlas.test", "motDePasse", "Ciel-Bleu-Montagne-7",
                                "accepteConditions", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.utilisateur.codeRole").value("ADMINISTRATEUR"))
                .andExpect(jsonPath("$.utilisateur.organisation.nom").value("Atlas Logistique"));

        mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", "salma@atlas.test", "motDePasse", "Ciel-Bleu-Montagne-7"))))
                .andExpect(status().isOk());
    }

    @Test
    void inscriptionSansAcceptationDesConditionsEstRefusee() throws Exception {
        // Case non cochée (false) ou champ absent (appel direct de l'API) : 400, aucun compte créé.
        for (Map<String, Object> requete : java.util.List.<Map<String, Object>>of(
                Map.of("nomOrganisation", "Atlas Logistique", "emailOrganisation", "contact@atlas.test",
                        "nom", "Bennani", "prenom", "Salma", "email", "salma@atlas.test", "motDePasse", "Ciel-Bleu-Montagne-7",
                        "accepteConditions", false),
                Map.of("nomOrganisation", "Atlas Logistique", "emailOrganisation", "contact@atlas.test",
                        "nom", "Bennani", "prenom", "Salma", "email", "salma@atlas.test", "motDePasse", "Ciel-Bleu-Montagne-7"))) {
            mvc.perform(post("/api/v1/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(corps(requete)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.champs.accepteConditions", containsString("accepter les conditions")));
        }
        mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", "salma@atlas.test", "motDePasse", "Ciel-Bleu-Montagne-7"))))
                .andExpect(status().isUnauthorized());
    }
}
