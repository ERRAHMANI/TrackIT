package com.trackit.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Les autres tests tournent sur H2, avec un schéma créé par Hibernate : ils ne vérifient ni les
 * migrations Flyway ni le comportement propre à MySQL. Ce test démarre un vrai MySQL 8 dans un
 * conteneur (Testcontainers), applique TOUTES les migrations comme en production (schéma V1 à V8 et
 * données de démonstration), laisse Hibernate valider le schéma (ddl-auto: validate), puis rejoue
 * quelques parcours sensibles sur cette base.
 *
 * Sans Docker (par exemple un agent Jenkins Windows sans Docker Desktop), la classe est ignorée au
 * lieu d'échouer ; elle s'exécute dans GitHub Actions, où Docker est disponible.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        // Configuration de production (application.yml), sauf les secrets et les dossiers de travail.
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        // Indépendant d'un éventuel .env local : schéma ET données de démonstration, comme en production de démo.
        "spring.flyway.locations=classpath:db/migration,classpath:db/demo",
        "trackit.jwt.secret=cle-de-test-uniquement-0123456789-abcdefghijklmnopqrstuvwxyz",
        "trackit.stripe.secret-key=",
        "trackit.sla.escalade-active=false",
        "trackit.stripe.webhook-secret=",
        "trackit.storage.uploads-dir=./target/test-uploads-mysql",
        "trackit.storage.reports-dir=./target/test-reports-mysql"
})
class MigrationsMySqlTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    /** Compte de la base de démonstration (migration V3), voir README section 12. */
    private static final String OPERATEUR_DEMO = "younes.errahmani@trackit.demo";
    private static final String MOT_DE_PASSE_DEMO = "TrackIT@2026";

    @Autowired
    private Flyway flyway;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void toutesLesMigrationsSontAppliqueesEtLeSchemaEstValide() {
        // Le contexte Spring n'a démarré que si Hibernate a validé le schéma produit par Flyway.
        MigrationInfo[] appliquees = flyway.info().applied();
        assertThat(Arrays.stream(appliquees).map(m -> m.getVersion().getVersion()))
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
        assertThat(Arrays.stream(appliquees).allMatch(m -> m.getState().isApplied() && !m.getState().isFailed())).isTrue();
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void laMigrationV6RattacheChaqueAnomalieAuxReferentielsDeSonOrganisation() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM anomalie", Long.class)).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM type_anomalie WHERE id_organisation IS NULL", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM priorite WHERE id_organisation IS NULL", Long.class)).isZero();
        Long incoherentes = jdbc.queryForObject("""
                SELECT COUNT(*) FROM anomalie a
                JOIN systeme_it s ON s.id_systeme = a.id_systeme
                JOIN type_anomalie t ON t.id_type = a.id_type
                JOIN priorite p ON p.id_priorite = a.id_priorite
                WHERE t.id_organisation <> s.id_organisation OR p.id_organisation <> s.id_organisation""", Long.class);
        assertThat(incoherentes).isZero();
    }

    @Test
    void uneInscriptionCreeSesReferentielsSurMySql() throws Exception {
        String reponse = mvc.perform(post("/api/v1/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nomOrganisation", "Atlas Logistique MySQL",
                                "emailOrganisation", "contact@atlas-mysql.test", "nom", "Bennani", "prenom", "Salma",
                                "email", "salma@atlas-mysql.test", "motDePasse", "Ciel-Bleu-Montagne-7", "accepteConditions", true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String jeton = "Bearer " + json.readTree(reponse).get("jeton").asText();
        mvc.perform(get("/api/v1/types").header("Authorization", jeton))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(8)));
        mvc.perform(get("/api/v1/priorites").header("Authorization", jeton))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void unChangementDeMotDePasseRevoqueLAncienJetonSurMySql() throws Exception {
        // Vérifie la précision à la milliseconde de mot_de_passe_modifie_le (DATETIME(3), migration V7).
        String ancienJeton = connexion(OPERATEUR_DEMO, MOT_DE_PASSE_DEMO);
        mvc.perform(put("/api/v1/profil/mot-de-passe").header("Authorization", ancienJeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("motDePasseActuel", MOT_DE_PASSE_DEMO,
                                "nouveauMotDePasse", "Ciel-Bleu-Montagne-7"))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/profil").header("Authorization", ancienJeton))
                .andExpect(status().isUnauthorized());
        String nouveauJeton = connexion(OPERATEUR_DEMO, "Ciel-Bleu-Montagne-7");
        mvc.perform(get("/api/v1/profil").header("Authorization", nouveauJeton))
                .andExpect(status().isOk());
    }

    private String connexion(String email, String motDePasse) throws Exception {
        String reponse = mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "motDePasse", motDePasse))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(reponse).get("jeton").asText();
    }
}
