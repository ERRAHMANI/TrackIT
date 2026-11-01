package com.trackit.dto;

public record AuthResponse(String jeton, String type, long expireDansSecondes, UtilisateurResponse utilisateur) {
}
