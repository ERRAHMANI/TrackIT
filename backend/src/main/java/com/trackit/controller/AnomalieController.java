package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.*;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.service.AnomalieService;
import com.trackit.service.CommentaireService;
import com.trackit.service.HistoriqueConsultationService;
import com.trackit.service.PieceJointeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiVersion.V1 + "/anomalies")
@Validated
@Tag(name = "Anomalies", description = "Déclaration, consultation, recherche et traitement des anomalies")
public class AnomalieController {

    /** Colonnes de tri exposées → propriétés JPA. */
    private static final Map<String, String> TRIS = Map.of(
            "id", "id",
            "titre", "titre",
            "dateCreation", "dateCreation",
            "priorite", "priorite.niveau",
            "statut", "statut.id",
            "systeme", "systeme.nom",
            "type", "type.nom");

    private final AnomalieService anomalieService;
    private final CommentaireService commentaireService;
    private final HistoriqueConsultationService historiqueService;
    private final PieceJointeService pieceJointeService;

    public AnomalieController(AnomalieService anomalieService, CommentaireService commentaireService,
                              HistoriqueConsultationService historiqueService, PieceJointeService pieceJointeService) {
        this.anomalieService = anomalieService;
        this.commentaireService = commentaireService;
        this.historiqueService = historiqueService;
        this.pieceJointeService = pieceJointeService;
    }

    @GetMapping
    @Operation(summary = "Rechercher les anomalies",
            description = "Recherche paginée, filtrée et triée en base. Le texte porte sur le titre, la description, le système et le numéro (#012).")
    public PageResponse<AnomalieResume> rechercher(
            @Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
            @Parameter(description = "Texte recherché") @RequestParam(required = false) String q,
            @RequestParam(required = false) Long idStatut,
            @RequestParam(required = false) Long idPriorite,
            @RequestParam(required = false) Long idType,
            @RequestParam(required = false) Long idSysteme,
            @RequestParam(required = false) Long idOperateur,
            @Parameter(description = "Uniquement les anomalies sans opérateur") @RequestParam(required = false) Boolean nonAffectee,
            @Parameter(description = "Uniquement les anomalies que j'ai déclarées") @RequestParam(required = false) Boolean mesDeclarations,
            @Parameter(description = "Uniquement les anomalies qui me sont affectées") @RequestParam(required = false) Boolean affecteesAMoi,
            @Parameter(description = "Créées à partir du (aaaa-mm-jj)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @Parameter(description = "Créées jusqu'au (aaaa-mm-jj)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int taille,
            @Parameter(description = "id, titre, dateCreation, priorite, statut, systeme, type") @RequestParam(defaultValue = "dateCreation") String tri,
            @Parameter(description = "asc ou desc") @RequestParam(defaultValue = "desc") String sens) {
        String propriete = TRIS.getOrDefault(tri, "dateCreation");
        Sort sort = Sort.by("asc".equalsIgnoreCase(sens) ? Sort.Direction.ASC : Sort.Direction.DESC, propriete)
                .and(Sort.by(Sort.Direction.DESC, "id"));
        AnomalieCritere critere = new AnomalieCritere(q, idStatut, idPriorite, idType, idSysteme, idOperateur,
                nonAffectee, mesDeclarations, affecteesAMoi, dateDebut, dateFin);
        return anomalieService.rechercher(courant, critere, PageRequest.of(page, taille, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une anomalie", description = "Inclut les actions autorisées pour l'utilisateur courant.")
    public AnomalieDetail obtenir(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                  @PathVariable Long id) {
        return anomalieService.obtenir(courant, id);
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATEUR')")
    @Operation(summary = "Déclarer une anomalie", description = "Titre, description, système, type et priorité sont obligatoires. "
            + "L'auteur, la date et le statut « Nouvelle » sont déterminés automatiquement.")
    @ApiResponse(responseCode = "201", description = "Anomalie créée")
    public ResponseEntity<AnomalieDetail> declarer(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                                   @Valid @RequestBody AnomalieRequest requete) {
        AnomalieDetail cree = anomalieService.declarer(courant, requete);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(cree.id()).toUri()).body(cree);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'RESPONSABLE_TECHNIQUE')")
    @Operation(summary = "Modifier / classifier une anomalie", description = "Chaque changement est tracé dans l'historique.")
    public AnomalieDetail modifier(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                   @PathVariable Long id, @Valid @RequestBody AnomalieRequest requete) {
        return anomalieService.modifier(courant, id, requete);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'RESPONSABLE_TECHNIQUE', 'ADMINISTRATEUR')")
    @Operation(summary = "Changer le statut", description = "Transitions : Nouvelle → En cours → Résolue → Fermée, et Résolue → En cours (réouverture).")
    public AnomalieDetail changerStatut(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                        @PathVariable Long id, @Valid @RequestBody ChangementStatutRequest requete) {
        return anomalieService.changerStatut(courant, id, requete);
    }

    @PatchMapping("/{id}/priorite")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'RESPONSABLE_TECHNIQUE')")
    @Operation(summary = "Prioriser une anomalie")
    public AnomalieDetail changerPriorite(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                          @PathVariable Long id, @Valid @RequestBody ChangementPrioriteRequest requete) {
        return anomalieService.changerPriorite(courant, id, requete);
    }

    @PatchMapping("/{id}/affectation")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'RESPONSABLE_TECHNIQUE')")
    @Operation(summary = "Affecter / réaffecter une anomalie",
            description = "Responsable technique : affecte à n'importe quel opérateur actif. Opérateur : prise en charge pour lui-même d'une anomalie non affectée.")
    public AnomalieDetail affecter(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                   @PathVariable Long id, @Valid @RequestBody AffectationRequest requete) {
        return anomalieService.affecter(courant, id, requete);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une anomalie", description = "Supprime aussi ses commentaires, pièces jointes, historique et notifications.")
    public void supprimer(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant, @PathVariable Long id) {
        anomalieService.supprimer(courant, id);
    }

    // ------------------------------------------------------------------ Commentaires

    @GetMapping("/{id}/commentaires")
    @Operation(summary = "Lister les commentaires")
    public List<CommentaireResponse> commentaires(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                                  @PathVariable Long id) {
        return commentaireService.lister(courant, id);
    }

    @PostMapping("/{id}/commentaires")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'RESPONSABLE_TECHNIQUE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajouter un commentaire")
    public CommentaireResponse commenter(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                         @PathVariable Long id, @Valid @RequestBody CommentaireRequest requete) {
        return commentaireService.ajouter(courant, id, requete);
    }

    // ------------------------------------------------------------------ Historique

    @GetMapping("/{id}/historique")
    @Operation(summary = "Consulter l'historique des modifications")
    public List<HistoriqueResponse> historique(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                               @PathVariable Long id) {
        return historiqueService.lister(courant, id);
    }

    // ------------------------------------------------------------------ Pièces jointes

    @GetMapping("/{id}/pieces-jointes")
    @Operation(summary = "Lister les pièces jointes")
    public List<PieceJointeResponse> piecesJointes(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                                   @PathVariable Long id) {
        return pieceJointeService.lister(courant, id);
    }

    @PostMapping(value = "/{id}/pieces-jointes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OPERATEUR', 'RESPONSABLE_TECHNIQUE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Joindre un fichier", description = "10 Mo maximum ; formats : pdf, png, jpg, gif, txt, log, csv, json, xml, docx, xlsx, zip.")
    public PieceJointeResponse joindre(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                       @PathVariable Long id, @RequestPart("fichier") MultipartFile fichier) {
        return pieceJointeService.ajouter(courant, id, fichier);
    }
}
