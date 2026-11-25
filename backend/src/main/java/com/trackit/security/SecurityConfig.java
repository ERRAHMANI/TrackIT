package com.trackit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackit.config.ApiVersion;
import com.trackit.config.TrackItProperties;
import com.trackit.dto.ErreurResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] ROUTES_PUBLIQUES = {
            ApiVersion.V1 + "/auth/connexion",
            ApiVersion.V1 + "/auth/inscription",
            ApiVersion.V1 + "/auth/deconnexion",
            ApiVersion.V1 + "/stripe/webhook",
            ApiVersion.V1 + "/public/**",
            "/error"
    };

    /** Documentation de l'API, publique seulement si elle est activée (SWAGGER_ENABLED=true). */
    private static final String[] ROUTES_DOCUMENTATION = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};

    /**
     * Politique de sécurité du contenu (CSP) : le client web ne charge que ses propres fichiers (aucun script,
     * style ni gestionnaire d'événement en ligne, aucun eval) et n'appelle que l'API de la même origine.
     * Stripe Checkout est une simple redirection : il n'exige aucune source supplémentaire.
     */
    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; "
            + "form-action 'self'; frame-ancestors 'none'";

    /** Swagger UI utilise des scripts et styles en ligne : la CSP stricte ne lui est pas appliquée. */
    private static final OrRequestMatcher DOCUMENTATION_API = new OrRequestMatcher(
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/v3/api-docs/**"));

    /** Fichiers du client web, servis par le backend en production (même origine que l'API). */
    private static final String[] FICHIERS_CLIENT = {
            "/", "/index.html", "/config.js", "/favicon.ico", "/css/**", "/js/**", "/assets/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                                   ObjectMapper objectMapper, JournalSecurite journal,
                                                   @Value("${springdoc.api-docs.enabled:false}") boolean documentationActive) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                // En plus des en-têtes par défaut (HSTS, X-Frame-Options, X-Content-Type-Options) :
                // n'envoyer que l'origine aux autres sites, désactiver les fonctions du navigateur inutiles
                // et limiter les sources de contenu (CSP).
                .headers(h -> h
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=()"))
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(new NegatedRequestMatcher(DOCUMENTATION_API),
                                new StaticHeadersWriter("Content-Security-Policy", CONTENT_SECURITY_POLICY))))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(ROUTES_PUBLIQUES).permitAll()
                            .requestMatchers(HttpMethod.GET, FICHIERS_CLIENT).permitAll()
                            .requestMatchers(HttpMethod.GET, ApiVersion.V1 + "/abonnements/offres").permitAll();
                    if (documentationActive) {
                        auth.requestMatchers(ROUTES_DOCUMENTATION).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            // Un jeton fourni mais refusé (invalide, expiré, compte supprimé) est un événement de sécurité.
                            if (req.getHeader(HttpHeaders.AUTHORIZATION) != null || CookieSession.lire(req).isPresent()) {
                                journal.jetonRefuse(req);
                            }
                            ecrireErreur(objectMapper, req, res,
                                    HttpStatus.UNAUTHORIZED, "Authentification requise : jeton absent, invalide ou expiré.");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            journal.accesRefuse(req);
                            ecrireErreur(objectMapper, req, res,
                                    HttpStatus.FORBIDDEN, "Votre rôle ne permet pas d'effectuer cette action.");
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UtilisateurDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(TrackItProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", CookieSession.ENTETE_CLIENT));
        // Le client web local (port 5173) envoie le cookie de session à l'API (port 8080).
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Content-Disposition", ApiVersion.ENTETE));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void ecrireErreur(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                                     HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), new ErreurResponse(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI(), null));
    }
}
