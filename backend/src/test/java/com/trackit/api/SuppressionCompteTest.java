package com.trackit.api;

import com.trackit.entity.Anomalie;
import com.trackit.entity.RoleCode;
import com.trackit.entity.StatutCode;
import com.trackit.entity.Utilisateur;
import com.trackit.repository.AnomalieRepository;
import com.trackit.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Suppression d'un compte : suppression physique sans trace, suppression logique (anonymisation) sinon. */
class SuppressionCompteTest extends TestIntegrationBase {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private AnomalieRepository anomalieRepository;

    @Test
    void compteSansTraceEstSupprimeDefinitivement() throws Exception {
        Long id = donnees.operateur2.getId();
        mvc.perform(delete("/api/v1/utilisateurs/" + id).header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR)))
                .andExpect(status().isNoContent());
        assertThat(utilisateurRepository.findById(id)).isEmpty();
    }

    @Test
    void compteAvecTracesEstAnonymiseEtSesAnomaliesConservees() throws Exception {
        Utilisateur operateur = donnees.comptes.get(RoleCode.OPERATEUR);
        Anomalie anomalie = donnees.anomalie("Panne imprimante", donnees.erp, StatutCode.NOUVELLE, operateur, null);

        mvc.perform(delete("/api/v1/utilisateurs/" + operateur.getId()).header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR)))
                .andExpect(status().isNoContent());

        Utilisateur apres = utilisateurRepository.findById(operateur.getId()).orElseThrow();
        assertThat(apres.isSupprime()).isTrue();
        assertThat(apres.isActif()).isFalse();
        assertThat(apres.getNom()).isEqualTo("SUPPRIMÉ");
        assertThat(apres.getEmail()).isEqualTo("supprime-" + operateur.getId() + "@trackit.invalid");
        assertThat(anomalieRepository.findById(anomalie.getId())).get()
                .extracting(a -> a.getAuteur().getId()).isEqualTo(operateur.getId());
    }

    @Test
    void compteAnonymiseDisparaitEtNePeutPlusSeConnecter() throws Exception {
        Utilisateur operateur = donnees.comptes.get(RoleCode.OPERATEUR);
        donnees.anomalie("Lenteurs réseau", donnees.reseau, StatutCode.NOUVELLE, operateur, null);
        String admin = donnees.jeton(RoleCode.ADMINISTRATEUR);

        mvc.perform(delete("/api/v1/utilisateurs/" + operateur.getId()).header("Authorization", admin))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/utilisateurs/" + operateur.getId()).header("Authorization", admin))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", "operateur@org-a.test", "motDePasse", DonneesTest.MOT_DE_PASSE))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adresseDUnCompteAnonymiseEstLiberee() throws Exception {
        Utilisateur operateur = donnees.comptes.get(RoleCode.OPERATEUR);
        donnees.anomalie("Erreur de synchronisation", donnees.erp, StatutCode.NOUVELLE, operateur, null);
        String admin = donnees.jeton(RoleCode.ADMINISTRATEUR);
        mvc.perform(delete("/api/v1/utilisateurs/" + operateur.getId()).header("Authorization", admin))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/utilisateurs").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("nom", "Martin", "prenom", "Léa", "email", "operateur@org-a.test",
                                "motDePasse", "Nouvelle-Equipe-Support-42", "idRole", operateur.getRole().getId(), "actif", true))))
                .andExpect(status().isCreated());
    }

    @Test
    void desinscriptionExigeLeMotDePasseActuel() throws Exception {
        mvc.perform(post("/api/v1/profil/suppression").header("Authorization", donnees.jeton(RoleCode.OPERATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("motDePasse", "mauvais-mot-de-passe"))))
                .andExpect(status().isBadRequest());
        assertThat(utilisateurRepository.findById(donnees.comptes.get(RoleCode.OPERATEUR).getId()))
                .get().extracting(Utilisateur::isSupprime).isEqualTo(false);
    }

    @Test
    void desinscriptionAnonymiseLeCompteEtInvalideSonJeton() throws Exception {
        Utilisateur operateur = donnees.comptes.get(RoleCode.OPERATEUR);
        donnees.anomalie("Serveur de fichiers saturé", donnees.erp, StatutCode.NOUVELLE, operateur, null);
        String jeton = donnees.jeton(operateur);

        mvc.perform(post("/api/v1/profil/suppression").header("Authorization", jeton)
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("motDePasse", DonneesTest.MOT_DE_PASSE))))
                .andExpect(status().isNoContent());

        assertThat(utilisateurRepository.findById(operateur.getId())).get()
                .extracting(Utilisateur::isSupprime).isEqualTo(true);
        mvc.perform(get("/api/v1/anomalies").header("Authorization", jeton)).andExpect(status().isUnauthorized());
    }

    @Test
    void dernierAdministrateurNePeutPasSeDesinscrire() throws Exception {
        mvc.perform(post("/api/v1/profil/suppression").header("Authorization", donnees.jeton(RoleCode.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("motDePasse", DonneesTest.MOT_DE_PASSE))))
                .andExpect(status().isConflict());
    }
}
