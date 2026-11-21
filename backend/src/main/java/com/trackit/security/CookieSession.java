package com.trackit.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie qui transporte le jeton JWT pour le client web (action P13, OWASP A07).
 * <ul>
 *   <li>{@code HttpOnly} : le JavaScript de la page ne peut pas le lire ; une faille XSS ne peut donc
 *       pas voler le jeton pour l'utiliser ailleurs ;</li>
 *   <li>{@code SameSite=Strict} : le navigateur ne l'envoie jamais depuis un autre site (CSRF) ;</li>
 *   <li>{@code Secure} en HTTPS (production) ; {@code Path=/api} : il n'accompagne que les appels à l'API.</li>
 * </ul>
 * « Se souvenir de moi » coché : cookie persistant jusqu'à l'expiration du jeton ; sinon cookie de
 * session, effacé à la fermeture du navigateur. L'en-tête {@code Authorization: Bearer} reste accepté
 * pour les clients de l'API (Swagger, tests).
 */
public final class CookieSession {

    public static final String NOM = "TRACKIT_SESSION";

    /**
     * En-tête exigé, en plus du cookie, sur les requêtes qui modifient des données. Un autre site ne
     * peut pas l'ajouter sans l'accord CORS du serveur : seconde protection contre le CSRF.
     */
    public static final String ENTETE_CLIENT = "X-TrackIT-Client";

    private static final String CHEMIN = "/api";

    private CookieSession() {
    }

    public static ResponseCookie creer(String jeton, long dureeSecondes, boolean persistant, boolean https) {
        ResponseCookie.ResponseCookieBuilder cookie = base(jeton, https);
        if (persistant) {
            cookie.maxAge(Duration.ofSeconds(dureeSecondes));
        }
        return cookie.build();
    }

    /** Cookie vide expiré : le navigateur supprime le cookie de session (déconnexion). */
    public static ResponseCookie effacer(boolean https) {
        return base("", https).maxAge(Duration.ZERO).build();
    }

    public static Optional<String> lire(HttpServletRequest requete) {
        Cookie[] cookies = requete.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> NOM.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank())
                .map(Cookie::getValue)
                .findFirst();
    }

    private static ResponseCookie.ResponseCookieBuilder base(String valeur, boolean https) {
        return ResponseCookie.from(NOM, valeur)
                .httpOnly(true)
                .secure(https)
                .sameSite("Strict")
                .path(CHEMIN);
    }
}
