package com.trackit.api;

import com.trackit.entity.RoleCode;
import com.trackit.repository.PaiementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Abonnement Premium en mode simulation (aucune clé Stripe dans le profil de test). */
class AbonnementTest extends TestIntegrationBase {

    @Autowired
    private PaiementRepository paiementRepository;

    @Test
    void seulLAdministrateurPeutPayer() throws Exception {
        mvc.perform(post("/api/v1/abonnements/paiement").header("Authorization", donnees.jeton(RoleCode.ANALYSTE))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("codeOffre", "PREMIUM_MENSUEL"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void paiementSimuleReussiActiveLePremium() throws Exception {
        String admin = donnees.jeton(RoleCode.ADMINISTRATEUR);
        String reference = demarrerPaiement(admin);

        simulerSucces(admin, reference);

        mvc.perform(get("/api/v1/abonnements/etat").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premiumActif").value(true));
    }

    @Test
    void deuxProlongationsSuccessivesSEnchainentSansChevauchement() throws Exception {
        String admin = donnees.jeton(RoleCode.ADMINISTRATEUR);
        simulerSucces(admin, demarrerPaiement(admin));
        simulerSucces(admin, demarrerPaiement(admin));

        LocalDate aujourdHui = LocalDate.now();
        mvc.perform(get("/api/v1/abonnements/historique").header("Authorization", admin))
                .andExpect(status().isOk())
                // Le plus récent commence le lendemain de la fin du premier
                .andExpect(jsonPath("$.abonnements[0].dateDebut").value(aujourdHui.plusMonths(1).toString()))
                .andExpect(jsonPath("$.abonnements[1].dateDebut").value(aujourdHui.toString()))
                .andExpect(jsonPath("$.abonnements[1].dateFin").value(aujourdHui.plusMonths(1).minusDays(1).toString()));
    }

    @Test
    void paiementAbandonnePlusDe24HeuresEstAnnule() throws Exception {
        String admin = donnees.jeton(RoleCode.ADMINISTRATEUR);
        String reference = demarrerPaiement(admin);
        var paiement = paiementRepository.findByReferenceStripe(reference).orElseThrow();
        paiement.setDatePaiement(LocalDateTime.now().minusHours(25));
        paiementRepository.save(paiement);

        mvc.perform(get("/api/v1/abonnements/historique").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paiements[0].statut").value("ANNULE"))
                .andExpect(jsonPath("$.abonnements[0].statut").value("ANNULE"));
    }

    private void simulerSucces(String jeton, String reference) throws Exception {
        mvc.perform(post("/api/v1/abonnements/paiement/simulation").header("Authorization", jeton)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("referenceSession", reference, "resultat", "SUCCES"))))
                .andExpect(status().isOk());
    }

    private String demarrerPaiement(String jeton) throws Exception {
        String reponse = mvc.perform(post("/api/v1/abonnements/paiement").header("Authorization", jeton)
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("codeOffre", "PREMIUM_MENSUEL"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulation").value(true))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(reponse).get("referenceSession").asText();
    }
}
