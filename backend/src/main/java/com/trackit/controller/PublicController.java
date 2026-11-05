package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.ChiffresPublicsResponse;
import com.trackit.entity.StatutCode;
import com.trackit.repository.AnomalieRepository;
import com.trackit.repository.OrganisationRepository;
import com.trackit.repository.SystemeItRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/public")
@Tag(name = "Public", description = "Données de la page d'accueil, sans authentification")
public class PublicController {

    private final OrganisationRepository organisationRepository;
    private final AnomalieRepository anomalieRepository;
    private final SystemeItRepository systemeItRepository;

    public PublicController(OrganisationRepository organisationRepository, AnomalieRepository anomalieRepository,
                            SystemeItRepository systemeItRepository) {
        this.organisationRepository = organisationRepository;
        this.anomalieRepository = anomalieRepository;
        this.systemeItRepository = systemeItRepository;
    }

    @GetMapping("/chiffres")
    @SecurityRequirements
    @Transactional(readOnly = true)
    @Operation(summary = "Chiffres clés de la plateforme")
    public ChiffresPublicsResponse chiffres() {
        long total = anomalieRepository.count();
        long traitees = anomalieRepository.countByStatutNomIn(
                List.of(StatutCode.RESOLUE.getLibelle(), StatutCode.FERMEE.getLibelle()));
        Double taux = total == 0 ? null : Math.round(1000.0 * traitees / total) / 10.0;
        return new ChiffresPublicsResponse(organisationRepository.count(), traitees, taux, systemeItRepository.count());
    }
}
