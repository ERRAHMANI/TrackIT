package com.trackit.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Règles de robustesse des mots de passe (NIST SP 800-63B) :
 * <ol>
 *   <li>au moins {@value #LONGUEUR_MIN} caractères ;</li>
 *   <li>au plus {@value #OCTETS_MAX} octets en UTF-8 : BCrypt ignore ce qui dépasse ;</li>
 *   <li>pas un mot de passe courant (liste {@code securite/mots-de-passe-interdits.txt}) ;</li>
 *   <li>pas un motif trivial : un seul caractère répété ou un petit bloc répété (« abcabcabcabc ») ;</li>
 *   <li>pas le nom, le prénom ou l'adresse e-mail de l'utilisateur (contrôlé par le service).</li>
 * </ol>
 * Les règles 1 à 4 s'appliquent aux nouveaux mots de passe seulement : les mots de passe existants
 * restent valables jusqu'à leur prochain changement.
 */
public final class PolitiqueMotDePasse {

    public static final int LONGUEUR_MIN = 12;
    public static final int OCTETS_MAX = 72;

    private static final Pattern BLOC_REPETE = Pattern.compile("(.{1,4})\\1+");
    private static final Set<String> INTERDITS = charger("securite/mots-de-passe-interdits.txt");

    private PolitiqueMotDePasse() {
    }

    /** Retourne le message d'erreur, ou {@code null} si le mot de passe respecte la politique. */
    public static String verifier(String motDePasse) {
        if (motDePasse.codePointCount(0, motDePasse.length()) < LONGUEUR_MIN) {
            return "Le mot de passe doit contenir au moins " + LONGUEUR_MIN + " caractères.";
        }
        if (motDePasse.getBytes(StandardCharsets.UTF_8).length > OCTETS_MAX) {
            return "Le mot de passe est trop long (" + OCTETS_MAX + " octets au maximum).";
        }
        String normalise = motDePasse.toLowerCase(Locale.ROOT);
        if (INTERDITS.contains(normalise) || BLOC_REPETE.matcher(normalise).matches()) {
            return "Ce mot de passe est trop courant ou trop facile à deviner.";
        }
        return null;
    }

    /**
     * Vrai si le mot de passe contient une information personnelle de l'utilisateur : le nom, le prénom,
     * l'adresse e-mail ou un de ses morceaux (partie avant le @ découpée sur . _ -), d'au moins 4 caractères.
     */
    public static boolean contientInformationPersonnelle(String motDePasse, String email, String... noms) {
        if (motDePasse == null) {
            return false;
        }
        String mdp = motDePasse.toLowerCase(Locale.ROOT);
        Set<String> mots = new HashSet<>();
        if (email != null && !email.isBlank()) {
            String local = email.toLowerCase(Locale.ROOT).split("@", 2)[0];
            mots.add(local);
            mots.addAll(Arrays.asList(local.split("[._\\-+]")));
        }
        Arrays.stream(noms).filter(Objects::nonNull).map(n -> n.trim().toLowerCase(Locale.ROOT)).forEach(n -> {
            mots.add(n);
            mots.addAll(Arrays.asList(n.split("[\\s\\-']+")));
        });
        return mots.stream().anyMatch(m -> m.length() >= 4 && mdp.contains(m));
    }

    private static Set<String> charger(String ressource) {
        Set<String> liste = new HashSet<>();
        InputStream flux = PolitiqueMotDePasse.class.getClassLoader().getResourceAsStream(ressource);
        if (flux == null) {
            throw new IllegalStateException("Liste des mots de passe interdits introuvable : " + ressource);
        }
        try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(flux, StandardCharsets.UTF_8))) {
            lecteur.lines().map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(l -> l.toLowerCase(Locale.ROOT)).forEach(liste::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Set.copyOf(liste);
    }
}
