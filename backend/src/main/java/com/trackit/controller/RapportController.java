package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.ErreurResponse;
import com.trackit.dto.PageResponse;
import com.trackit.dto.RapportRequest;
import com.trackit.dto.RapportResponse;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.service.rapport.RapportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(ApiVersion.V1 + "/rapports")
@Validated
@PreAuthorize("hasRole('ANALYSTE')")
@Tag(name = "Rapports", description = "Génération et export PDF / Excel (Analyste, fonctionnalité Premium)")
public class RapportController {

    private final RapportService rapportService;

    public RapportController(RapportService rapportService) {
        this.rapportService = rapportService;
    }

    @GetMapping
    @Operation(summary = "Lister les rapports générés par l'organisation")
    public PageResponse<RapportResponse> lister(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "10") @Min(1) @Max(100) int taille) {
        return rapportService.lister(courant, page, taille);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Générer un rapport", description = "Types : SYNTHESE, LISTE_ANOMALIES ; formats : PDF, EXCEL.")
    @ApiResponse(responseCode = "402", description = "Abonnement Premium requis", content = @Content(schema = @Schema(implementation = ErreurResponse.class)))
    public RapportResponse generer(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                   @Valid @RequestBody RapportRequest requete) {
        return rapportService.generer(courant, requete);
    }

    @GetMapping("/{id}/fichier")
    @Operation(summary = "Télécharger le fichier d'un rapport")
    public ResponseEntity<Resource> telecharger(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                                @PathVariable Long id) {
        RapportService.FichierRapport fichier = rapportService.telecharger(courant, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.typeContenu()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fichier.nomFichier(), StandardCharsets.UTF_8).build().toString())
                .body(fichier.ressource());
    }
}
