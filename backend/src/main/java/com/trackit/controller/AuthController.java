package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.AuthResponse;
import com.trackit.dto.ConnexionRequest;
import com.trackit.dto.ErreurResponse;
import com.trackit.dto.InscriptionRequest;
import com.trackit.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/auth")
@Tag(name = "Authentification", description = "Connexion par e-mail / mot de passe et obtention du jeton JWT")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/connexion")
    @Operation(summary = "Se connecter", description = "Retourne un jeton JWT à transmettre dans l'en-tête Authorization: Bearer <jeton>.")
    @ApiResponse(responseCode = "200", description = "Authentification réussie")
    @ApiResponse(responseCode = "400", description = "Champs manquants ou invalides",
            content = @Content(schema = @Schema(implementation = ErreurResponse.class)))
    @ApiResponse(responseCode = "401", description = "Identifiants incorrects ou compte désactivé",
            content = @Content(schema = @Schema(implementation = ErreurResponse.class)))
    @ApiResponse(responseCode = "429", description = "Trop d'échecs : compte ou adresse IP bloqué temporairement (force brute)",
            content = @Content(schema = @Schema(implementation = ErreurResponse.class)))
    public AuthResponse connexion(@Valid @RequestBody ConnexionRequest requete, HttpServletRequest http) {
        // Derrière le proxy de Railway, l'adresse du client est reprise de X-Forwarded-For
        // (server.forward-headers-strategy: native).
        return authService.connecter(requete, http.getRemoteAddr());
    }

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscrire une organisation", description = "Crée l'organisation et son premier compte Administrateur, puis le connecte.")
    @ApiResponse(responseCode = "201", description = "Organisation et administrateur créés")
    @ApiResponse(responseCode = "409", description = "E-mail déjà utilisé",
            content = @Content(schema = @Schema(implementation = ErreurResponse.class)))
    public AuthResponse inscription(@Valid @RequestBody InscriptionRequest requete, HttpServletRequest http) {
        return authService.inscrire(requete, http.getRemoteAddr());
    }
}