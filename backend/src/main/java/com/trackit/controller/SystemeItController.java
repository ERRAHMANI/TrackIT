package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.SystemeItRequest;
import com.trackit.dto.SystemeItResponse;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.service.SystemeItService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/systemes")
@Tag(name = "Systèmes IT", description = "Systèmes informatiques de l'organisation")
public class SystemeItController {

    private final SystemeItService systemeItService;

    public SystemeItController(SystemeItService systemeItService) {
        this.systemeItService = systemeItService;
    }

    @GetMapping
    @Operation(summary = "Lister les systèmes IT")
    public List<SystemeItResponse> lister(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                          @Parameter(description = "Uniquement les systèmes actifs") @RequestParam(defaultValue = "false") boolean actifs) {
        return systemeItService.lister(courant, actifs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un système IT")
    public SystemeItResponse obtenir(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                     @PathVariable Long id) {
        return systemeItService.obtenir(courant, id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un système IT")
    public SystemeItResponse creer(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                   @Valid @RequestBody SystemeItRequest requete) {
        return systemeItService.creer(courant, requete);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @Operation(summary = "Modifier un système IT")
    public SystemeItResponse modifier(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                      @PathVariable Long id, @Valid @RequestBody SystemeItRequest requete) {
        return systemeItService.modifier(courant, id, requete);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un système IT", description = "Refusé (409) si des anomalies le référencent.")
    public void supprimer(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant, @PathVariable Long id) {
        systemeItService.supprimer(courant, id);
    }
}
