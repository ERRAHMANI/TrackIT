package com.trackit.exception;

import org.springframework.http.HttpStatus;

/** Erreur métier ou d'accès traduite en réponse HTTP par {@link GlobalExceptionHandler}. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException introuvable(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException interdit(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    public static ApiException conflit(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    /** Message commun aux deux contrôles de modification simultanée d'une anomalie (verrou optimiste). */
    public static final String MODIFIEE_ENTRE_TEMPS = "Cette anomalie a été modifiée par quelqu'un d'autre "
            + "depuis que vous l'avez ouverte. Rechargez la fiche pour voir la dernière version.";

    public static ApiException modifieeEntreTemps() {
        return conflit(MODIFIEE_ENTRE_TEMPS);
    }

    public static ApiException invalide(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    /** Trop d'échecs de connexion : le message est le même que le compte existe ou non. */
    public static ApiException tropDeTentatives(long minutes) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "Trop de tentatives de connexion. Réessayez dans " + minutes + (minutes > 1 ? " minutes." : " minute."));
    }

    /** Trop d'inscriptions depuis la même adresse IP. */
    public static ApiException tropDInscriptions(long minutes) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "Trop d'inscriptions depuis cette adresse. Réessayez dans " + minutes + (minutes > 1 ? " minutes." : " minute."));
    }

    /** Mot de passe refusé car il contient le nom, le prénom ou l'adresse e-mail de l'utilisateur. */
    public static ApiException motDePassePersonnel() {
        return invalide("Le mot de passe ne doit contenir ni le nom, ni le prénom, ni l'adresse e-mail de l'utilisateur.");
    }

    public static ApiException premiumRequis() {
        return new ApiException(HttpStatus.PAYMENT_REQUIRED,
                "Cette fonctionnalité nécessite un abonnement Premium actif pour votre organisation.");
    }
}
