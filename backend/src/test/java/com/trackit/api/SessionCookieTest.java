package com.trackit.api;

import com.trackit.entity.RoleCode;
import com.trackit.security.CookieSession;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Jeton du client web dans un cookie HttpOnly (action P13) : illisible par le JavaScript, donc hors
 * de portée d'une faille XSS ; SameSite=Strict et en-tête X-TrackIT-Client contre le CSRF.
 */
class SessionCookieTest extends TestIntegrationBase {

    private static final String EMAIL = "operateur@org-a.test";

    @Test
    void laConnexionDuClientWebPoseUnCookieHttpOnlyEtNeRenvoiePasLeJeton() throws Exception {
        mvc.perform(post("/api/v1/auth/connexion").header(CookieSession.ENTETE_CLIENT, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", EMAIL, "motDePasse", DonneesTest.MOT_DE_PASSE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton").value(nullValue()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(startsWith(CookieSession.NOM + "="),
                        containsString("HttpOnly"), containsString("SameSite=Strict"), containsString("Path=/api"),
                        not(containsString("Max-Age")))));
    }

    @Test
    void seSouvenirDeMoiRendLeCookiePersistant() throws Exception {
        mvc.perform(post("/api/v1/auth/connexion").header(CookieSession.ENTETE_CLIENT, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", EMAIL, "motDePasse", DonneesTest.MOT_DE_PASSE, "seSouvenir", true))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=")));
    }

    @Test
    void lesClientsDeLApiRecoiventToujoursLeJetonDansLeCorps() throws Exception {
        mvc.perform(post("/api/v1/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content(corps(Map.of("email", EMAIL, "motDePasse", DonneesTest.MOT_DE_PASSE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton", not(emptyOrNullString())));
    }

    @Test
    void leCookieSuffitPourLire() throws Exception {
        mvc.perform(get("/api/v1/profil").cookie(cookie(RoleCode.OPERATEUR)))
                .andExpect(status().isOk());
    }

    @Test
    void uneModificationAvecLeCookieSansEnTeteClientEstRefusee() throws Exception {
        // Ce que ferait un autre site (CSRF) : le cookie seul ne suffit pas pour modifier des données.
        mvc.perform(post("/api/v1/systemes").cookie(cookie(RoleCode.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(corps(systeme())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/systemes").cookie(cookie(RoleCode.ADMINISTRATEUR)).header(CookieSession.ENTETE_CLIENT, "1")
                        .contentType(MediaType.APPLICATION_JSON).content(corps(systeme())))
                .andExpect(status().isCreated());
    }

    @Test
    void laDeconnexionEffaceLeCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/deconnexion"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(startsWith(CookieSession.NOM + "=;"),
                        containsString("Max-Age=0"), containsString("HttpOnly"))));
    }

    private Cookie cookie(RoleCode role) {
        return new Cookie(CookieSession.NOM, donnees.jeton(role).substring("Bearer ".length()));
    }

    private static Map<String, Object> systeme() {
        Map<String, Object> m = new HashMap<>();
        m.put("nom", "Serveur de fichiers");
        m.put("description", "Partages réseau");
        m.put("actif", true);
        return m;
    }
}
