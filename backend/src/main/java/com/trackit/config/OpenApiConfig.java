package com.trackit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SCHEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI trackItOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TrackIT — API REST")
                        .version(ApiVersion.NUMERO)
                        .contact(new Contact().name("Équipe TrackIT").email("contact@trackit.demo"))
                        .description("""
                                API de gestion, de suivi et de traitement des anomalies informatiques.

                                **Versioning** — la version majeure figure dans l'URL : toutes les routes commencent par
                                `/api/v1`. La version complète est renvoyée dans l'en-tête de réponse `X-API-Version`.
                                Une évolution incompatible sera publiée sous `/api/v2`, `/api/v1` restant disponible.

                                **Authentification** — JWT (Bearer) :
                                1. appeler **POST /api/v1/auth/connexion** avec un compte existant
                                   (en local : comptes de démonstration, voir README section 12) ;
                                2. copier la valeur `jeton` de la réponse ;
                                3. cliquer sur **Authorize** et coller le jeton.

                                Les droits dépendent du rôle : Utilisateur, Opérateur, Responsable technique, Analyste,
                                Administrateur. Les routes marquées sans cadenas sont publiques (données ouvertes).

                                **Erreurs** — format commun `ErreurResponse` : 400 validation (détail par champ dans `champs`),
                                401 non authentifié, 402 Premium requis, 403 rôle insuffisant, 404 introuvable,
                                409 règle métier, 413 fichier trop volumineux."""))
                // URL relative : les requêtes « Try it out » partent vers le serveur qui affiche cette page
                // (localhost:8080 en développement, l'adresse publique en production).
                .servers(List.of(new Server().url("/").description("Serveur courant")))
                .components(new Components().addSecuritySchemes(SCHEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Jeton obtenu via POST /api/v1/auth/connexion, valable 8 heures par défaut.")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_JWT));
    }

    /**
     * Documente les réponses d'erreur communes (format ErreurResponse) sur chaque opération,
     * sans écraser les réponses déjà décrites dans les contrôleurs.
     */
    @Bean
    public OpenApiCustomizer reponsesErreurCommunes() {
        return openApi -> openApi.getPaths().forEach((chemin, item) -> item.readOperationsMap().forEach((methode, operation) -> {
            boolean publique = operation.getSecurity() != null && operation.getSecurity().isEmpty();
            boolean ecriture = methode != PathItem.HttpMethod.GET;
            boolean parametres = operation.getRequestBody() != null || (operation.getParameters() != null
                    && operation.getParameters().stream().anyMatch(p -> "query".equals(p.getIn())));
            if (parametres) ajouter(operation, "400", "Requête invalide : champs obligatoires manquants ou valeurs incorrectes (détail dans « champs »)");
            if (!publique) {
                ajouter(operation, "401", "Jeton absent, invalide ou expiré");
                ajouter(operation, "403", "Rôle insuffisant ou action non autorisée sur cette ressource");
            }
            if (chemin.contains("{")) ajouter(operation, "404", "Ressource introuvable (ou appartenant à une autre organisation)");
            if (ecriture && !publique) ajouter(operation, "409", "Règle métier non respectée (transition interdite, doublon, élément utilisé…)");
        }));
    }

    private static void ajouter(Operation operation, String code, String description) {
        ApiResponses reponses = operation.getResponses();
        if (reponses.containsKey(code)) {
            return;
        }
        reponses.addApiResponse(code, new ApiResponse().description(description).content(new Content()
                .addMediaType("application/json", new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErreurResponse")))));
    }
}