package com.trackit.api;

import com.trackit.entity.RoleCode;
import com.trackit.security.JournalSecurite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Journal de sécurité : les événements sensibles sont écrits, jamais les mots de passe. */
@ExtendWith(OutputCaptureExtension.class)
class JournalSecuriteTest extends TestIntegrationBase {

    @Test
    void echecDeConnexionEstJournaliseSansLeMotDePasse(CapturedOutput sortie) throws Exception {
        mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", "operateur@org-a.test", "motDePasse", "Secret-Errone-4242"))))
                .andExpect(status().isUnauthorized());
        assertThat(sortie.getOut()).contains("evenement=CONNEXION_ECHOUEE email=operateur@org-a.test")
                .contains("raison=identifiants_incorrects")
                .doesNotContain("Secret-Errone-4242");
    }

    @Test
    void connexionReussieEstJournaliseeSansLeJeton(CapturedOutput sortie) throws Exception {
        String reponse = mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", "operateur@org-a.test", "motDePasse", DonneesTest.MOT_DE_PASSE))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String jeton = reponse.replaceAll(".*\"jeton\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        assertThat(sortie.getOut()).contains("evenement=CONNEXION_REUSSIE email=operateur@org-a.test")
                .doesNotContain(DonneesTest.MOT_DE_PASSE);
        if (!jeton.equals(reponse)) {
            assertThat(sortie.getOut()).doesNotContain(jeton);
        }
    }

    @Test
    void accesRefuseEstJournalise(CapturedOutput sortie) throws Exception {
        mvc.perform(get("/api/v1/utilisateurs").header("Authorization", donnees.jeton(RoleCode.OPERATEUR)))
                .andExpect(status().isForbidden());
        assertThat(sortie.getOut()).contains("evenement=ACCES_REFUSE")
                .contains("(OPERATEUR)").contains("uri=/api/v1/utilisateurs");
    }

    @Test
    void jetonFalsifieEstJournalise(CapturedOutput sortie) throws Exception {
        mvc.perform(get("/api/v1/anomalies").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.e30.signature-invalide"))
                .andExpect(status().isUnauthorized());
        assertThat(sortie.getOut()).contains("evenement=JETON_REFUSE methode=GET uri=/api/v1/anomalies");
    }

    @Test
    void suppressionDUnCompteEstJournalisee(CapturedOutput sortie) throws Exception {
        Long id = donnees.operateur2.getId();
        mvc.perform(delete("/api/v1/utilisateurs/" + id).header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR)))
                .andExpect(status().isNoContent());
        assertThat(sortie.getOut()).contains("evenement=COMPTE_SUPPRIME")
                .contains("cible=" + id);
    }

    @Test
    void retoursALaLigneSontNeutralises() {
        assertThat(JournalSecurite.nettoyer("pirate@test.be\r\nevenement=CONNEXION_REUSSIE"))
                .doesNotContain("\n").doesNotContain("\r")
                .isEqualTo("pirate@test.be__evenement=CONNEXION_REUSSIE");
        assertThat(JournalSecurite.nettoyer(null)).isEqualTo("-");
        assertThat(JournalSecurite.nettoyer("x".repeat(200))).hasSize(121);
    }
}
