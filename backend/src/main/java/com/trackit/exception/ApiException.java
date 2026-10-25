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

    public static ApiException invalide(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException premiumRequis() {
        return new ApiException(HttpStatus.PAYMENT_REQUIRED,
                "Cette fonctionnalité nécessite un abonnement Premium actif pour votre organisation.");
    }
}
