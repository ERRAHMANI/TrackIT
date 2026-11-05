package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.*;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.service.AbonnementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/abonnements")
@Tag(name = "Abonnement Premium", description = "Offres, paiement Stripe (mode test) et état de l'abonnement")
public class AbonnementController {

    private final AbonnementService abonnementService;

    public AbonnementController(AbonnementService abonnementService) {
        this.abonnementService = abonnementService;
    }

    @GetMapping("/offres")
    @SecurityRequirements
    @Operation(summary = "Lister les offres Premium (public)")
    public List<OffreResponse> offres() {
        return abonnementService.offres();
    }

    @GetMapping("/etat")
    @Operation(summary = "État Premium de l'organisation")
    public EtatPremiumResponse etat(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant) {
        return abonnementService.etat(courant);
    }

    @GetMapping("/historique")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Historique des abonnements et paiements")
    public HistoriqueAbonnementResponse historique(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant) {
        return abonnementService.historique(courant);
    }

    @PostMapping("/paiement")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Souscrire : créer une session de paiement",
            description = "Retourne l'URL Stripe Checkout (ou la page de simulation si aucune clé Stripe n'est configurée).")
    public CheckoutResponse payer(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                  @Valid @RequestBody CheckoutRequest requete) {
        return abonnementService.demarrerPaiement(courant, requete);
    }

    @PostMapping("/paiement/confirmation")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Confirmer un paiement au retour de Stripe", description = "L'état est relu auprès de Stripe.")
    public ResultatPaiementResponse confirmer(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                              @Valid @RequestBody ConfirmationPaiementRequest requete) {
        return abonnementService.confirmer(courant, requete);
    }

    @PostMapping("/paiement/annulation")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Annuler un paiement en attente")
    public ResultatPaiementResponse annuler(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                            @Valid @RequestBody ConfirmationPaiementRequest requete) {
        return abonnementService.annuler(courant, requete);
    }

    @PostMapping("/paiement/simulation")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Simuler le résultat d'un paiement", description = "Disponible uniquement sans clé Stripe (démonstration).")
    public ResultatPaiementResponse simuler(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                            @Valid @RequestBody SimulationPaiementRequest requete) {
        return abonnementService.simuler(courant, requete);
    }
}
