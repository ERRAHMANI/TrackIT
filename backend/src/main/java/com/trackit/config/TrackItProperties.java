package com.trackit.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "trackit")
public record TrackItProperties(
        @Valid Jwt jwt,
        Cors cors,
        String frontendUrl,
        Storage storage,
        Sla sla,
        Stripe stripe) {

    public record Jwt(
            @NotBlank(message = "JWT_SECRET doit être défini (voir .env.example)")
            @Size(min = 32, message = "JWT_SECRET doit contenir au moins 32 caractères")
            String secret,
            long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Storage(String uploadsDir, String reportsDir) {
    }

    /** Délai cible (en heures) par niveau de priorité (1 = Basse … 4 = Critique). */
    public record Sla(Map<Integer, Integer> heuresParNiveau) {

        public int heuresPour(int niveau) {
            return heuresParNiveau.getOrDefault(niveau, 72);
        }
    }

    public record Stripe(String secretKey, String webhookSecret, String currency, List<Offre> offres) {

        public boolean modeSimulation() {
            return secretKey == null || secretKey.isBlank();
        }
    }

    public record Offre(String code, String libelle, BigDecimal montant, int dureeMois) {
    }
}
