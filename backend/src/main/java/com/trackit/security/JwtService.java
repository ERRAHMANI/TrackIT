package com.trackit.security;

import com.trackit.config.TrackItProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    /**
     * Instant d'émission à la milliseconde. La revendication standard « iat » n'a qu'une précision
     * à la seconde : elle ne permet pas de distinguer un jeton émis juste avant un changement de
     * mot de passe d'un jeton émis juste après, dans la même seconde.
     */
    private static final String EMIS_MS = "emis_ms";

    private final SecretKey cle;
    private final long expirationMinutes;

    public JwtService(TrackItProperties properties) {
        this.cle = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = properties.jwt().expirationMinutes();
    }

    public String genererJeton(UtilisateurConnecte utilisateur) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(utilisateur.email())
                .claim("uid", utilisateur.id())
                .claim("role", utilisateur.role().name())
                .claim("org", utilisateur.organisationId())
                .issuedAt(Date.from(maintenant))
                .claim(EMIS_MS, maintenant.toEpochMilli())
                .expiration(Date.from(maintenant.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(cle)
                .compact();
    }

    public long getExpirationSecondes() {
        return expirationMinutes * 60;
    }

    /** Contenu utile d'un jeton dont la signature et l'expiration ont été vérifiées. */
    public record JetonVerifie(String email, Instant emisLe) {

        /** Vrai si le jeton a été émis avant le dernier changement de mot de passe du compte. */
        public boolean estAnterieurA(LocalDateTime motDePasseModifieLe) {
            return motDePasseModifieLe != null
                    && emisLe.isBefore(motDePasseModifieLe.atZone(ZoneId.systemDefault()).toInstant());
        }
    }

    /** Retourne l'e-mail et l'instant d'émission si le jeton est valide et non expiré. */
    public Optional<JetonVerifie> verifier(String jeton) {
        try {
            Claims claims = Jwts.parser().verifyWith(cle).build().parseSignedClaims(jeton).getPayload();
            if (claims.getSubject() == null || claims.getIssuedAt() == null) {
                return Optional.empty();
            }
            Long emisMs = claims.get(EMIS_MS, Long.class);
            // Jeton sans « emis_ms » (émis avant cette version) : précision de « iat », à la seconde.
            Instant emisLe = emisMs != null ? Instant.ofEpochMilli(emisMs) : claims.getIssuedAt().toInstant();
            return Optional.of(new JetonVerifie(claims.getSubject(), emisLe));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
