package com.trackit.api;

import com.trackit.entity.RoleCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Politique de mot de passe (NIST SP 800-63B) : longueur, mots de passe courants, informations personnelles. */
class MotDePasseTest extends TestIntegrationBase {

    private static final String ROBUSTE = "Ciel-Bleu-Montagne-7";

    @Test
    void motDePasseDeMoinsDe12CaracteresEstRefuse() throws Exception {
        inscription("Court2026!").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.motDePasse", containsString("au moins 12 caractères")));
    }

    @Test
    void motDePasseCourantEstRefuse() throws Exception {
        inscription("Azertyuiop123").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.motDePasse", containsString("trop courant")));
    }

    @Test
    void motDePasseRepetitifEstRefuse() throws Exception {
        inscription("abcabcabcabc").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.motDePasse", containsString("trop courant")));
    }

    @Test
    void motDePasseDePlusDe72OctetsEstRefuse() throws Exception {
        inscription("é".repeat(40)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.motDePasse", containsString("72 octets")));
    }

    @Test
    void motDePasseContenantLeNomEstRefuse() throws Exception {
        inscription("Bennani-Atlas-2026").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ni le nom")));
    }

    @Test
    void motDePasseRobusteEstAccepte() throws Exception {
        inscription(ROBUSTE).andExpect(status().isCreated());
    }

    @Test
    void changementDeMotDePasseAppliqueLaPolitique() throws Exception {
        String jeton = donnees.jeton(RoleCode.OPERATEUR);
        changement(jeton, "Court2026!").andExpect(status().isBadRequest());
        changement(jeton, ROBUSTE).andExpect(status().isNoContent());
    }

    @Test
    void reinitialisationParLAdministrateurAppliqueLaPolitique() throws Exception {
        String admin = donnees.jeton(RoleCode.ADMINISTRATEUR);
        String url = "/api/v1/utilisateurs/" + donnees.operateur2.getId() + "/mot-de-passe";
        mvc.perform(put(url).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("nouveauMotDePasse", "123456789012"))))
                .andExpect(status().isBadRequest());
        mvc.perform(put(url).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("nouveauMotDePasse", ROBUSTE))))
                .andExpect(status().isNoContent());
    }

    private ResultActions inscription(String motDePasse) throws Exception {
        return mvc.perform(post("/api/v1/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("nomOrganisation", "Atlas Logistique", "emailOrganisation", "contact@atlas.test",
                        "nom", "Bennani", "prenom", "Salma", "email", "salma@atlas.test", "motDePasse", motDePasse))));
    }

    private ResultActions changement(String jeton, String nouveau) throws Exception {
        return mvc.perform(put("/api/v1/profil/mot-de-passe").header("Authorization", jeton)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("motDePasseActuel", DonneesTest.MOT_DE_PASSE, "nouveauMotDePasse", nouveau))));
    }
}
