package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.*;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/utilisateurs")
@Validated
@Tag(name = "Utilisateurs", description = "Gestion des comptes de l'organisation (Administrateur)")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Lister les utilisateurs")
    public PageResponse<UtilisateurResponse> lister(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                                    @RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Long idRole,
                                                    @RequestParam(required = false) Boolean actif,
                                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int taille) {
        return utilisateurService.lister(courant, q, idRole, actif, PageRequest.of(page, taille, Sort.by("nom", "prenom")));
    }

    @GetMapping("/operateurs")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'RESPONSABLE_TECHNIQUE')")
    @Operation(summary = "Lister les opérateurs actifs", description = "Utilisé pour l'affectation des anomalies.")
    public List<UtilisateurResume> operateurs(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant) {
        return utilisateurService.operateursActifs(courant);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Consulter un utilisateur")
    public UtilisateurResponse obtenir(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                       @PathVariable Long id) {
        return utilisateurService.obtenir(courant, id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un utilisateur", description = "Le mot de passe est stocké haché (BCrypt).")
    public UtilisateurResponse creer(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                     @Valid @RequestBody UtilisateurCreationRequest requete) {
        return utilisateurService.creer(courant, requete);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Modifier un utilisateur, son rôle ou son état")
    public UtilisateurResponse modifier(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                        @PathVariable Long id, @Valid @RequestBody UtilisateurModificationRequest requete) {
        return utilisateurService.modifier(courant, id, requete);
    }

    @PutMapping("/{id}/mot-de-passe")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Réinitialiser le mot de passe d'un utilisateur")
    public void reinitialiserMotDePasse(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                        @PathVariable Long id, @Valid @RequestBody MotDePasseRequest requete) {
        utilisateurService.reinitialiserMotDePasse(courant, id, requete);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un utilisateur", description = "Refusé (409) si le compte est lié à des données : il faut alors le désactiver.")
    public void supprimer(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant, @PathVariable Long id) {
        utilisateurService.supprimer(courant, id);
    }
}
