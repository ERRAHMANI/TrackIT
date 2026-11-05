package com.trackit.controller;

import com.trackit.config.ApiVersion;
import com.trackit.service.AbonnementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/stripe")
@Tag(name = "Abonnement Premium")
public class StripeWebhookController {

    private final AbonnementService abonnementService;

    public StripeWebhookController(AbonnementService abonnementService) {
        this.abonnementService = abonnementService;
    }

    @PostMapping("/webhook")
    @SecurityRequirements
    @Operation(summary = "Webhook Stripe", description = "Appelé par Stripe ; la signature Stripe-Signature est vérifiée.")
    public ResponseEntity<Void> webhook(@RequestBody String charge,
                                        @RequestHeader("Stripe-Signature") String signature) {
        abonnementService.traiterWebhook(charge, signature);
        return ResponseEntity.ok().build();
    }
}
