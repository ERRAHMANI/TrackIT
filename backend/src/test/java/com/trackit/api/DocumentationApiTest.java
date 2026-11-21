package com.trackit.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sans SWAGGER_ENABLED=true (production), la documentation de l'API n'est ni générée ni publique. */
@TestPropertySource(properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
class DocumentationApiTest extends TestIntegrationBase {

    @Test
    void documentationDesactiveeNEstPasAccessibleAnonymement() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
    }
}
