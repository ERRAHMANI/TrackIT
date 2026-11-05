package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.*;
import com.trackit.service.ReferentielService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1)
@Tag(name = "Référentiels", description = "Rôles, types d'anomalie, priorités et statuts")
public class ReferentielController {

    private final ReferentielService referentielService;

    public ReferentielController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    @GetMapping("/roles")
    @Operation(summary = "Lister les rôles")
    public List<RoleResponse> roles() {
        return referentielService.roles();
    }

    @GetMapping("/types")
    @Operation(summary = "Lister les types d'anomalie")
    public List<TypeAnomalieResponse> types() {
        return referentielService.types();
    }

    @PostMapping("/types")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un type d'anomalie")
    public TypeAnomalieResponse creerType(@Valid @RequestBody TypeAnomalieRequest requete) {
        return referentielService.creerType(requete);
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Modifier un type d'anomalie")
    public TypeAnomalieResponse modifierType(@PathVariable Long id, @Valid @RequestBody TypeAnomalieRequest requete) {
        return referentielService.modifierType(id, requete);
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un type d'anomalie non utilisé")
    public void supprimerType(@PathVariable Long id) {
        referentielService.supprimerType(id);
    }

    @GetMapping("/priorites")
    @Operation(summary = "Lister les priorités")
    public List<PrioriteResponse> priorites() {
        return referentielService.priorites();
    }

    @PostMapping("/priorites")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une priorité")
    public PrioriteResponse creerPriorite(@Valid @RequestBody PrioriteRequest requete) {
        return referentielService.creerPriorite(requete);
    }

    @PutMapping("/priorites/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Modifier une priorité")
    public PrioriteResponse modifierPriorite(@PathVariable Long id, @Valid @RequestBody PrioriteRequest requete) {
        return referentielService.modifierPriorite(id, requete);
    }

    @DeleteMapping("/priorites/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une priorité non utilisée")
    public void supprimerPriorite(@PathVariable Long id) {
        referentielService.supprimerPriorite(id);
    }

    @GetMapping("/statuts")
    @Operation(summary = "Lister les quatre statuts officiels")
    public List<StatutResponse> statuts() {
        return referentielService.statuts();
    }

    @PutMapping("/statuts/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Modifier la description d'un statut", description = "Les statuts ne peuvent être ni créés ni supprimés.")
    public StatutResponse modifierStatut(@PathVariable Long id, @Valid @RequestBody StatutRequest requete) {
        return referentielService.modifierStatut(id, requete);
    }
}
