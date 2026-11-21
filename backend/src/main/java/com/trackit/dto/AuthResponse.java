package com.trackit.dto;

public record AuthResponse(String jeton, String type, long expireDansSecondes, UtilisateurResponse utilisateur) {

    /** Réponse du client web : le jeton n'est que dans le cookie HttpOnly, jamais lisible par le JavaScript. */
    public AuthResponse sansJeton() {
        return new AuthResponse(null, type, expireDansSecondes, utilisateur);
    }
}
