package com.trackit.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.security.ProtectionConnexion;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Chaque test s'exécute dans une transaction annulée à la fin : la base reste propre. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
abstract class TestIntegrationBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected DonneesTest donnees;

    @Autowired
    protected ProtectionConnexion protectionConnexion;

    @BeforeEach
    void preparerDonnees() {
        donnees.initialiser();
        // Les compteurs d'échecs de connexion sont en mémoire : chaque test repart de zéro.
        protectionConnexion.reinitialiser();
    }

    protected String corps(Object objet) throws Exception {
        return json.writeValueAsString(objet);
    }
}
