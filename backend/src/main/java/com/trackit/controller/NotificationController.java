package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.dto.NotificationResponse;
import com.trackit.dto.PageResponse;
import com.trackit.security.UtilisateurConnecte;
import com.trackit.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ApiVersion.V1 + "/notifications")
@Validated
@Tag(name = "Notifications", description = "Notifications de l'utilisateur connecté")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lister ses notifications")
    public PageResponse<NotificationResponse> lister(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "10") @Min(1) @Max(100) int taille) {
        return notificationService.lister(courant, page, taille);
    }

    @GetMapping("/non-lues")
    @Operation(summary = "Nombre de notifications non lues")
    public Map<String, Long> nonLues(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant) {
        return Map.of("nombre", notificationService.compterNonLues(courant));
    }

    @PatchMapping("/{id}/lue")
    @Operation(summary = "Marquer une notification comme lue")
    public NotificationResponse marquerLue(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant,
                                           @PathVariable Long id) {
        return notificationService.marquerLue(courant, id);
    }

    @PatchMapping("/lues")
    @Operation(summary = "Tout marquer comme lu")
    public Map<String, Integer> toutesLues(@Parameter(hidden = true) @AuthenticationPrincipal UtilisateurConnecte courant) {
        return Map.of("modifiees", notificationService.marquerToutesLues(courant));
    }
}
