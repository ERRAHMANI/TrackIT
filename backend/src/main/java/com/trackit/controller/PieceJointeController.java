package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.service.PieceJointeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(ApiVersion.V1 + "/pieces-jointes")
@Tag(name = "Anomalies")
public class PieceJointeController {

    private final PieceJointeService pieceJointeService;

    public PieceJointeController(PieceJointeService pieceJointeService) {
        this.pieceJointeService = pieceJointeService;
    }

    @GetMapping("/{id}/telechargement")
    @Operation(summary = "Télécharger une pièce jointe")
    public ResponseEntity<Resource> telecharger(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                                @PathVariable Long id) {
        PieceJointeService.Telechargement fichier = pieceJointeService.telecharger(courant, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fichier.typeFichier()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fichier.nomFichier(), StandardCharsets.UTF_8).build().toString())
                .body(fichier.ressource());
    }
}
