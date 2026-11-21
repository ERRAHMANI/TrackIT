package com.trackit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    /** Méthodes qui ne modifient rien : le cookie seul suffit. */
    private static final Set<String> METHODES_SURES = Set.of("GET", "HEAD", "OPTIONS");

    private final JwtService jwtService;
    private final UtilisateurDetailsService utilisateurDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UtilisateurDetailsService utilisateurDetailsService) {
        this.jwtService = jwtService;
        this.utilisateurDetailsService = utilisateurDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            jetonDeLaRequete(request)
                    .flatMap(jwtService::verifier)
                    .ifPresent(jeton -> authentifier(jeton, request));
        }
        chain.doFilter(request, response);
    }

    /**
     * En-tête Authorization (clients de l'API) en priorité, sinon cookie HttpOnly du client web. Avec
     * le cookie, une requête qui modifie des données doit aussi porter l'en-tête X-TrackIT-Client :
     * sans lui, elle reste anonyme (protection CSRF en plus de SameSite=Strict).
     */
    private static Optional<String> jetonDeLaRequete(HttpServletRequest request) {
        String entete = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (entete != null && entete.startsWith(PREFIXE)) {
            return Optional.of(entete.substring(PREFIXE.length()));
        }
        boolean modification = !METHODES_SURES.contains(request.getMethod());
        if (modification && request.getHeader(CookieSession.ENTETE_CLIENT) == null) {
            return Optional.empty();
        }
        return CookieSession.lire(request);
    }

    private void authentifier(JwtService.JetonVerifie jeton, HttpServletRequest request) {
        try {
            UtilisateurConnecte utilisateur = utilisateurDetailsService.loadUserByUsername(jeton.email());
            // Un compte désactivé après l'émission du jeton perd immédiatement l'accès.
            if (!utilisateur.isEnabled()) {
                return;
            }
            // Un jeton émis avant le dernier changement ou la dernière réinitialisation du mot de passe
            // est révoqué : la requête reste anonyme et sera rejetée (401, JETON_REFUSE dans le journal).
            if (jeton.estAnterieurA(utilisateur.motDePasseModifieLe())) {
                return;
            }
            var authentication = new UsernamePasswordAuthenticationToken(utilisateur, null, utilisateur.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException ignored) {
            // Jeton valide mais compte supprimé : la requête reste anonyme et sera rejetée (401).
        }
    }
}
