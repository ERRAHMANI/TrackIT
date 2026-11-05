package com.trackit.config;

/**
 * Version de l'API REST. Le numéro majeur figure dans l'URL (/api/v1/…) : une évolution
 * incompatible donnera lieu à un préfixe /api/v2 sans casser les clients existants.
 */
public final class ApiVersion {

    public static final String V1 = "/api/v1";
    public static final String NUMERO = "1.0.0";
    public static final String ENTETE = "X-API-Version";

    private ApiVersion() {
    }
}
