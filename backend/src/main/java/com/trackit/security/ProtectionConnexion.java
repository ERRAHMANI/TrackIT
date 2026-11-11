package com.trackit.security;

import com.trackit.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protection contre les attaques en force brute sur la connexion (OWASP A07).
 *
 * <p>Deux compteurs d'échecs, sur une fenêtre glissante :
 * <ul>
 *   <li>par adresse e-mail : après {@code maxEchecsCompte} échecs, le compte est bloqué ;</li>
 *   <li>par adresse IP : après {@code maxEchecsAdresse} échecs, toutes les tentatives de cette adresse
 *       sont bloquées, ce qui freine l'essai d'un même mot de passe sur de nombreux comptes.</li>
 * </ul>
 * Le blocage dure {@code dureeMinutes} minutes. Il s'applique aussi aux adresses e-mail qui n'existent
 * pas : la réponse ne révèle donc jamais si un compte existe. Une connexion réussie remet le compteur
 * du compte à zéro.
 *
 * <p>Les compteurs sont gardés en mémoire : ils sont propres à une instance de l'application et
 * repartent de zéro à son redémarrage, ce qui suffit pour une seule instance (une réplique sur Railway).
 */
@Component
public class ProtectionConnexion {

    private static final Logger LOG = LoggerFactory.getLogger(ProtectionConnexion.class);

    /** Au-delà, les compteurs expirés sont purgés pour borner la mémoire utilisée. */
    private static final int TAILLE_PURGE = 10_000;

    private final int maxEchecsCompte;
    private final int maxEchecsAdresse;
    private final Duration duree;
    private final Map<String, Compteur> compteurs = new ConcurrentHashMap<>();

    public ProtectionConnexion(@Value("${trackit.connexion.max-echecs-compte:5}") int maxEchecsCompte,
                               @Value("${trackit.connexion.max-echecs-adresse:20}") int maxEchecsAdresse,
                               @Value("${trackit.connexion.duree-minutes:15}") int dureeMinutes) {
        this.maxEchecsCompte = maxEchecsCompte;
        this.maxEchecsAdresse = maxEchecsAdresse;
        this.duree = Duration.ofMinutes(dureeMinutes);
    }

    /** Refuse la tentative (429) si le compte ou l'adresse IP est bloqué. À appeler avant de vérifier le mot de passe. */
    public void verifier(String email, String adresse) {
        Instant maintenant = Instant.now();
        Instant fin = plusTard(finBlocage(cleCompte(email), maintenant), finBlocage(cleAdresse(adresse), maintenant));
        if (fin != null) {
            long minutes = Math.max(1, (Duration.between(maintenant, fin).getSeconds() + 59) / 60);
            throw ApiException.tropDeTentatives(minutes);
        }
    }

    /** Enregistre un échec de connexion (mot de passe incorrect, compte inconnu ou désactivé). */
    public void enregistrerEchec(String email, String adresse) {
        Instant maintenant = Instant.now();
        purgerSiNecessaire(maintenant);
        if (incrementer(cleCompte(email), maxEchecsCompte, maintenant)) {
            LOG.warn("Connexion bloquée {} min pour le compte {} après {} échecs (adresse {})",
                    duree.toMinutes(), email, maxEchecsCompte, adresse);
        }
        if (incrementer(cleAdresse(adresse), maxEchecsAdresse, maintenant)) {
            LOG.warn("Connexion bloquée {} min pour l'adresse {} après {} échecs", duree.toMinutes(), adresse, maxEchecsAdresse);
        }
    }

    /** Une connexion réussie efface les échecs du compte (pas ceux de l'adresse IP). */
    public void enregistrerSucces(String email) {
        compteurs.remove(cleCompte(email));
    }

    /** Vide tous les compteurs (utilisé par les tests). */
    public void reinitialiser() {
        compteurs.clear();
    }

    // ------------------------------------------------------------------ Interne

    /** Retourne vrai si ce nouvel échec déclenche le blocage. */
    private boolean incrementer(String cle, int maximum, Instant maintenant) {
        boolean[] bloque = {false};
        compteurs.compute(cle, (k, c) -> {
            if (c == null || c.estExpire(maintenant)) {
                c = new Compteur(maintenant.plus(duree));
            }
            c.echecs++;
            if (c.echecs >= maximum && c.bloqueJusqua == null) {
                c.bloqueJusqua = maintenant.plus(duree);
                bloque[0] = true;
            }
            return c;
        });
        return bloque[0];
    }

    private Instant finBlocage(String cle, Instant maintenant) {
        Compteur c = compteurs.get(cle);
        if (c == null || c.bloqueJusqua == null) {
            return null;
        }
        if (!maintenant.isBefore(c.bloqueJusqua)) {
            compteurs.remove(cle, c);
            return null;
        }
        return c.bloqueJusqua;
    }

    private void purgerSiNecessaire(Instant maintenant) {
        if (compteurs.size() > TAILLE_PURGE) {
            compteurs.values().removeIf(c -> c.estExpire(maintenant));
        }
    }

    private static Instant plusTard(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private static String cleCompte(String email) {
        return "compte:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private static String cleAdresse(String adresse) {
        return "adresse:" + (adresse == null ? "inconnue" : adresse);
    }

    /** Échecs d'une clé sur la fenêtre en cours, et fin du blocage éventuel. */
    private static final class Compteur {
        private final Instant finFenetre;
        private int echecs;
        private Instant bloqueJusqua;

        private Compteur(Instant finFenetre) {
            this.finFenetre = finFenetre;
        }

        /** Un compteur expire à la fin de sa fenêtre, ou à la fin du blocage si celle-ci est plus tardive. */
        private boolean estExpire(Instant maintenant) {
            Instant fin = bloqueJusqua != null && bloqueJusqua.isAfter(finFenetre) ? bloqueJusqua : finFenetre;
            return !maintenant.isBefore(fin);
        }
    }
}
