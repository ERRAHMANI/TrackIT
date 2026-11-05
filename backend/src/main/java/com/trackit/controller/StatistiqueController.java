package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.StatistiquesResponse;
import com.trackit.dto.SupervisionResponse;
import com.trackit.dto.TableauDeBordResponse;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.service.StatistiqueService;
import com.trackit.service.SupervisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(ApiVersion.V1)
@Tag(name = "Statistiques et supervision", description = "Indicateurs calculés depuis la base de données")
public class StatistiqueController {

    private final StatistiqueService statistiqueService;
    private final SupervisionService supervisionService;

    public StatistiqueController(StatistiqueService statistiqueService, SupervisionService supervisionService) {
        this.statistiqueService = statistiqueService;
        this.supervisionService = supervisionService;
    }

    @GetMapping("/statistiques/tableau-de-bord")
    @Operation(summary = "Tableau de bord", description = "Indicateurs, évolution journalière, répartition par type et anomalies récentes.")
    public TableauDeBordResponse tableauDeBord(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return statistiqueService.tableauDeBord(courant, debut, fin);
    }

    @GetMapping("/statistiques")
    @PreAuthorize("hasAnyRole('ANALYSTE', 'RESPONSABLE_TECHNIQUE')")
    @Operation(summary = "Statistiques détaillées", description = "Répartitions, taux et durées de résolution, incidents récurrents.")
    public StatistiquesResponse statistiques(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return statistiqueService.statistiques(courant, debut, fin);
    }

    @GetMapping("/supervision")
    @PreAuthorize("hasRole('RESPONSABLE_TECHNIQUE')")
    @Operation(summary = "Supervision et contrôle des délais", description = "Anomalies ouvertes, retards par rapport aux délais cibles, charge des opérateurs.")
    public SupervisionResponse supervision(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant) {
        return supervisionService.superviser(courant);
    }
}
