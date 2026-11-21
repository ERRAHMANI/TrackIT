package com.trackit.api;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** En-têtes de sécurité HTTP ajoutés à toutes les réponses (voir le livrable 16, section 7.4). */
class EnTetesSecuriteTest extends TestIntegrationBase {

    @Test
    void reponsesContiennentLesEnTetesDeSecurite() throws Exception {
        mvc.perform(get("/api/v1/public/chiffres"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"));
    }

    @Test
    void contentSecurityPolicyStricteSurLApplication() throws Exception {
        mvc.perform(get("/api/v1/public/chiffres"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self';")))
                .andExpect(header().string("Content-Security-Policy", containsString("object-src 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy", not(containsString("unsafe-inline"))));
    }

    @Test
    void laDocumentationSwaggerNeRecoitPasLaCspStricte() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }

    @Test
    void hstsEnvoyeSeulementEnHttps() throws Exception {
        mvc.perform(get("/api/v1/public/chiffres").secure(true))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"));
        mvc.perform(get("/api/v1/public/chiffres"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }
}
