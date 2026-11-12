package com.trackit.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Journal des événements de sécurité (OWASP A09 « Security Logging and Monitoring Failures »).
 *
 * <p>Chaque événement est écrit sur une ligne par le logger dédié {@code trackit.securite}, au format
 * {@code evenement=NOM cle=valeur …}, facile à filtrer dans les journaux (Railway, console, fichier).
 *
 * <ul>
 *   <li>WARN : échecs de connexion, blocages (force brute), accès refusés (403), jetons refusés (401) ;</li>
 *   <li>INFO : connexions réussies, inscriptions, comptes créés, rôles modifiés, mots de passe changés ou
 *       réinitialisés, comptes supprimés, anonymisés ou désinscrits.</li>
 * </ul>
 *
 * <p>Ne sont <strong>jamais</strong> journalisés : les mots de passe, les jetons JWT, les clés. Les valeurs
 * venant de l'utilisateur sont nettoyées (retours à la ligne retirés, longueur bornée) pour empêcher
 * l'injection de fausses lignes dans le journal.
 */
@Component
public class JournalSecurite {

    private static final Logger LOG = LoggerFactory.getLogger("trackit.securite");
    private static final int LONGUEUR_MAX = 120;

    // ------------------------------------------------------------------ Connexion

    public void connexionReussie(String email, String adresseIp) {
        LOG.info("evenement=CONNEXION_REUSSIE email={} ip={}", nettoyer(email), nettoyer(adresseIp));
    }

    public void connexionEchouee(String email, String adresseIp, String raison) {
        LOG.warn("evenement=CONNEXION_ECHOUEE email={} ip={} raison={}", nettoyer(email), nettoyer(adresseIp), nettoyer(raison));
    }

    public void connexionRefuseeBlocage(String email, String adresseIp) {
        LOG.warn("evenement=CONNEXION_REFUSEE_BLOCAGE email={} ip={}", nettoyer(email), nettoyer(adresseIp));
    }

    public void compteBloque(String email, String adresseIp, int echecs, long minutes) {
        LOG.warn("evenement=COMPTE_BLOQUE email={} ip={} echecs={} duree_min={}", nettoyer(email), nettoyer(adresseIp), echecs, minutes);
    }

    public void adresseBloquee(String adresseIp, int echecs, long minutes) {
        LOG.warn("evenement=ADRESSE_BLOQUEE ip={} echecs={} duree_min={}", nettoyer(adresseIp), echecs, minutes);
    }

    // ------------------------------------------------------------------ Accès

    /** 403 : utilisateur authentifié dont le rôle ne permet pas l'action. */
    public void accesRefuse(HttpServletRequest requete) {
        LOG.warn("evenement=ACCES_REFUSE utilisateur={} methode={} uri={} ip={}", utilisateurCourant(),
                requete.getMethod(), nettoyer(requete.getRequestURI()), nettoyer(requete.getRemoteAddr()));
    }

    /** 401 alors qu'un jeton était fourni : jeton invalide, expiré, falsifié ou compte supprimé. */
    public void jetonRefuse(HttpServletRequest requete) {
        LOG.warn("evenement=JETON_REFUSE methode={} uri={} ip={}", requete.getMethod(),
                nettoyer(requete.getRequestURI()), nettoyer(requete.getRemoteAddr()));
    }

    // ------------------------------------------------------------------ Gestion des comptes

    /** Opération sensible sur un compte : auteur et cible sont désignés par leur identifiant. */
    public void operationCompte(String evenement, Long auteur, Long cible) {
        LOG.info("evenement={} auteur={} cible={}", evenement, auteur, cible);
    }

    public void roleModifie(Long auteur, Long cible, String ancien, String nouveau) {
        LOG.info("evenement=ROLE_MODIFIE auteur={} cible={} ancien={} nouveau={}", auteur, cible, nettoyer(ancien), nettoyer(nouveau));
    }

    public void inscription(Long organisation, Long administrateur, String adresseIp) {
        LOG.info("evenement=INSCRIPTION organisation={} administrateur={} ip={}", organisation, administrateur, nettoyer(adresseIp));
    }

    // ------------------------------------------------------------------ Outils

    /** Retire les retours à la ligne et les tabulations, et borne la longueur (anti-injection de logs). */
    public static String nettoyer(String valeur) {
        if (valeur == null) {
            return "-";
        }
        String propre = valeur.replaceAll("[\\r\\n\\t]", "_").replace(' ', '_');
        return propre.length() > LONGUEUR_MAX ? propre.substring(0, LONGUEUR_MAX) + "…" : propre;
    }

    private static String utilisateurCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UtilisateurConnecte u) {
            return u.id() + "(" + u.role() + ")";
        }
        return "anonyme";
    }
}
