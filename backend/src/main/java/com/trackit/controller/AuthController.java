package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.AuthResponse;
import com.trackit.dto.ConnexionRequest;
import com.trackit.dto.ErreurResponse;
import com.trackit.dto.InscriptionRequest;
import com.trackit.security.CookieSession;
import com.trackit.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
    @Operation(summary = "Se connecter", description = "Retourne un jeton JWT à transmettre dans l'en-tête Authorization: Bearer <jeton>. "
            + "Le client web reçoit aussi ce jeton dans un cookie HttpOnly (TRACKIT_SESSION), illisible par le JavaScript.")
    @ApiResponse(responseCode = "200", description = "Authentification réussie")
    @ApiResponse(responseCode = "400", description = "Champs manquants ou invalides",
            content = @Content(schema = @Schema(implementation = ErreurResponse.class)))
    @ApiResponse(responseCode = "401", description = "Identifiants incorrects ou compte désactivé",
            content = @Content(schema = @Schema(implementation = ErreurResponse.class)))
    @ApiResponse(responseCode = "429", description = "Trop d'échecs : compte ou adresse IP bloqué temporairement (force brute)",
            content = @Content(schema = @Schema(implementation = ErreurResponse.class)))
    public AuthResponse connexion(@Valid @RequestBody ConnexionRequest requete, HttpServletRequest http,
                                  HttpServletResponse reponseHttp) {
        // Derrière le proxy de Railway, l'adresse du client est reprise de X-Forwarded-For
        // (server.forward-headers-strategy: native).
        AuthResponse reponse = authService.connecter(requete, http.getRemoteAddr());
        return poserCookie(reponse, requete.persistant(), http, reponseHttp);
    }

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscrire une organisation", description = "Crée l'organisation et son premier compte Administrateur, puis le connecte.")
    @ApiResponse(responseCode = "201", description = "Organisation et administrateur créés")
    @ApiResponse(responseCode = "409", description = "E-mail déjà utilisé",
            content = @Content(schema = @Schema(implementation = ErreurResponse.class)))
    public AuthResponse inscription(@Valid @RequestBody InscriptionRequest requete, HttpServletRequest http,
                                    HttpServletResponse reponseHttp) {
        AuthResponse reponse = authService.inscrire(requete, http.getRemoteAddr());
        return poserCookie(reponse, false, http, reponseHttp);
    }

    @PostMapping("/deconnexion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Se déconnecter", description = "Supprime le cookie de session du client web.")
    @ApiResponse(responseCode = "204", description = "Cookie supprimé")
    public void deconnexion(HttpServletRequest http, HttpServletResponse reponseHttp) {
        reponseHttp.addHeader(HttpHeaders.SET_COOKIE, CookieSession.effacer(http.isSecure()).toString());
    }

    /**
     * Pose le cookie HttpOnly. Pour le client web (en-tête X-TrackIT-Client), le jeton est retiré du
     * corps de la réponse : il n'existe alors que dans le cookie. Les autres clients (Swagger, scripts)
     * le reçoivent dans le corps pour l'en-tête Authorization.
     */
    private static AuthResponse poserCookie(AuthResponse reponse, boolean persistant, HttpServletRequest http,
                                            HttpServletResponse reponseHttp) {
        reponseHttp.addHeader(HttpHeaders.SET_COOKIE, CookieSession.creer(reponse.jeton(),
                reponse.expireDansSecondes(), persistant, http.isSecure()).toString());
        return http.getHeader(CookieSession.ENTETE_CLIENT) != null ? reponse.sansJeton() : reponse;
    }
}