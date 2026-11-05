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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;
    private final UtilisateurDetailsService utilisateurDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UtilisateurDetailsService utilisateurDetailsService) {
        this.jwtService = jwtService;
        this.utilisateurDetailsService = utilisateurDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String entete = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (entete != null && entete.startsWith(PREFIXE)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.extraireEmail(entete.substring(PREFIXE.length())).ifPresent(email -> authentifier(email, request));
        }
        chain.doFilter(request, response);
    }

    private void authentifier(String email, HttpServletRequest request) {
        try {
            UtilisateurConnecte utilisateur = utilisateurDetailsService.loadUserByUsername(email);
            // Un compte désactivé après l'émission du jeton perd immédiatement l'accès.
            if (!utilisateur.isEnabled()) {
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
