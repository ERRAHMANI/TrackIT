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
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

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
                .expiration(Date.from(maintenant.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(cle)
                .compact();
    }

    public long getExpirationSecondes() {
        return expirationMinutes * 60;
    }

    /** Retourne l'e-mail du sujet si le jeton est valide et non expiré. */
    public Optional<String> extraireEmail(String jeton) {
        try {
            Claims claims = Jwts.parser().verifyWith(cle).build().parseSignedClaims(jeton).getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
