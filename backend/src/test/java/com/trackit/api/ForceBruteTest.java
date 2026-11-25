package com.trackit.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Protection de la connexion contre la force brute : 5 échecs par compte, 20 par adresse IP.
 * Limite des inscriptions publiques : 5 par adresse IP et par heure.
 */
class ForceBruteTest extends TestIntegrationBase {

    private static final String COMPTE = "operateur@org-a.test";

    @Test
    void cinqEchecsBloquentLeCompteMemeAvecLeBonMotDePasse() throws Exception {
        for (int i = 0; i < 5; i++) {
            connexion(COMPTE, "mauvais", "10.0.0.1").andExpect(status().isUnauthorized());
        }
        connexion(COMPTE, DonneesTest.MOT_DE_PASSE, "10.0.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("Trop de tentatives")));
    }

    @Test
    void connexionReussieRemetLeCompteurAZero() throws Exception {
        for (int i = 0; i < 4; i++) {
            connexion(COMPTE, "mauvais", "10.0.0.2").andExpect(status().isUnauthorized());
        }
        connexion(COMPTE, DonneesTest.MOT_DE_PASSE, "10.0.0.2").andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            connexion(COMPTE, "mauvais", "10.0.0.2").andExpect(status().isUnauthorized());
        }
        connexion(COMPTE, DonneesTest.MOT_DE_PASSE, "10.0.0.2").andExpect(status().isOk());
    }

    @Test
    void compteInexistantEstBloqueDeLaMemeFacon() throws Exception {
        for (int i = 0; i < 5; i++) {
            connexion("inconnu@org-a.test", "mauvais", "10.0.0.3").andExpect(status().isUnauthorized());
        }
        connexion("inconnu@org-a.test", "mauvais", "10.0.0.3").andExpect(status().isTooManyRequests());
    }

    @Test
    void blocageDUnCompteNeBloquePasLesAutres() throws Exception {
        for (int i = 0; i < 5; i++) {
            connexion(COMPTE, "mauvais", "10.0.0.4").andExpect(status().isUnauthorized());
        }
        connexion("analyste@org-a.test", DonneesTest.MOT_DE_PASSE, "10.0.0.4").andExpect(status().isOk());
    }

    @Test
    void vingtEchecsDepuisUneAdresseBloquentCetteAdresse() throws Exception {
        for (int i = 0; i < 20; i++) {
            connexion("victime" + i + "@org-a.test", "mauvais", "10.0.0.5").andExpect(status().isUnauthorized());
        }
        connexion("analyste@org-a.test", DonneesTest.MOT_DE_PASSE, "10.0.0.5").andExpect(status().isTooManyRequests());
        connexion("analyste@org-a.test", DonneesTest.MOT_DE_PASSE, "10.0.0.6").andExpect(status().isOk());
    }

    @Test
    void laSixiemeInscriptionDepuisLaMemeAdresseEstRefusee() throws Exception {
        for (int i = 1; i <= 5; i++) {
            inscription(i, "10.0.0.7").andExpect(status().isCreated());
        }
        inscription(6, "10.0.0.7")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("Trop d'inscriptions")));
        // Une autre adresse n'est pas concernée.
        inscription(7, "10.0.0.8").andExpect(status().isCreated());
    }

    private ResultActions inscription(int numero, String adresseIp) throws Exception {
        return mvc.perform(post("/api/v1/auth/inscription")
                .with(requete -> { requete.setRemoteAddr(adresseIp); return requete; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("nomOrganisation", "Organisation " + numero, "emailOrganisation", "contact" + numero + "@inscription.test",
                        "nom", "Dupont", "prenom", "Claire", "email", "claire" + numero + "@inscription.test",
                        "motDePasse", "Ciel-Bleu-Montagne-7", "accepteConditions", true))));
    }

    private ResultActions connexion(String email, String motDePasse, String adresseIp) throws Exception {
        return mvc.perform(post("/api/v1/auth/connexion")
                .with(requete -> { requete.setRemoteAddr(adresseIp); return requete; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", email, "motDePasse", motDePasse))));
    }
}
