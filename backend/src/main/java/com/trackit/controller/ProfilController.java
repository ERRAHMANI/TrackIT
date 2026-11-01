package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.ChangementMotDePasseRequest;
import com.trackit.dto.ProfilRequest;
import com.trackit.dto.UtilisateurResponse;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/profil")
@Tag(name = "Profil", description = "Informations personnelles de l'utilisateur connecté")
public class ProfilController {

    private final UtilisateurService utilisateurService;

    public ProfilController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @Operation(summary = "Consulter son profil")
    public UtilisateurResponse profil(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant) {
        return utilisateurService.profil(courant);
    }

    @PutMapping
    @Operation(summary = "Modifier ses informations personnelles", description = "Le rôle et l'organisation ne sont pas modifiables ici.")
    public UtilisateurResponse modifier(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                        @Valid @RequestBody ProfilRequest requete) {
        return utilisateurService.modifierProfil(courant, requete);
    }

    @PutMapping("/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Changer son mot de passe")
    public void changerMotDePasse(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                  @Valid @RequestBody ChangementMotDePasseRequest requete) {
        utilisateurService.changerMotDePasse(courant, requete);
    }
}
