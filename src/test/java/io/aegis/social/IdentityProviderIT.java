package io.aegis.social;

import static io.aegis.commons.testing.AegisJwtTest.jwtForTenant;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for the per-tenant identity-provider registry against real Postgres. Covers
 * scope-based authorization, preset application, per-preset validation, tenant isolation, and the
 * security-critical invariant that the client secret is never returned.
 */
@SpringBootTest
@Import(BrokerTestConfig.class)
class IdentityProviderIT {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void listing_is_scope_gated() throws Exception {
        mockMvc.perform(get("/api/v1/identity-providers")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/identity-providers")
                        .with(jwtForTenant("acme", "admin", "identity:users:read")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/identity-providers")
                        .with(jwtForTenant("acme", "admin", "idp:admin")))
                .andExpect(status().isOk());
    }

    @Test
    void catalog_lists_the_supported_providers() throws Exception {
        mockMvc.perform(get("/api/v1/identity-providers/catalog")
                        .with(jwtForTenant("acme", "admin", "idp:admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key=='GOOGLE')]").exists())
                .andExpect(jsonPath("$[?(@.key=='ENTRA')]").exists())
                .andExpect(jsonPath("$[?(@.key=='APPLE')]").exists())
                .andExpect(jsonPath("$[?(@.key=='GITHUB')]").exists())
                .andExpect(jsonPath("$[?(@.key=='SAML')]").exists());
    }

    @Test
    void create_google_applies_the_preset_and_never_returns_the_secret() throws Exception {
        String body = mockMvc.perform(post("/api/v1/identity-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerKey":"GOOGLE","alias":"google",
                                 "clientId":"g-client","clientSecret":"g-secret"}""")
                        .with(jwtForTenant("acme", "admin", "idp:admin")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocol").value("OIDC"))
                .andExpect(jsonPath("$.issuerUri").value("https://accounts.google.com"))
                .andExpect(jsonPath("$.scopes").value("openid email profile"))
                .andExpect(jsonPath("$.hasSecret").value(true))
                .andExpect(jsonPath("$.clientSecret").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("g-secret");
    }

    @Test
    void entra_templates_the_issuer_from_the_directory_and_requires_it() throws Exception {
        // Missing providerDirectory -> 400.
        mockMvc.perform(post("/api/v1/identity-providers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerKey":"ENTRA","alias":"entra","clientId":"c","clientSecret":"s"}""")
                        .with(jwtForTenant("acme", "admin", "idp:admin")))
                .andExpect(status().isBadRequest());

        // With it -> issuer is templated.
        mockMvc.perform(post("/api/v1/identity-providers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerKey":"ENTRA","alias":"entra","clientId":"c","clientSecret":"s",
                                 "providerDirectory":"dir-123"}""")
                        .with(jwtForTenant("acme", "admin", "idp:admin")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issuerUri").value("https://login.microsoftonline.com/dir-123/v2.0"));
    }

    @Test
    void github_is_oauth2_and_saml_needs_metadata_not_a_secret() throws Exception {
        mockMvc.perform(post("/api/v1/identity-providers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerKey":"GITHUB","alias":"github","clientId":"gh","clientSecret":"s"}""")
                        .with(jwtForTenant("bravo", "admin", "idp:admin")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocol").value("OAUTH2"));

        // SAML without metadata -> 400.
        mockMvc.perform(post("/api/v1/identity-providers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerKey":"SAML","alias":"corp-saml"}""")
                        .with(jwtForTenant("bravo", "admin", "idp:admin")))
                .andExpect(status().isBadRequest());

        // SAML with metadata -> 201 (no client id/secret required).
        mockMvc.perform(post("/api/v1/identity-providers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerKey":"SAML","alias":"corp-saml",
                                 "samlMetadataUrl":"https://idp.example/metadata"}""")
                        .with(jwtForTenant("bravo", "admin", "idp:admin")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocol").value("SAML"));
    }

    @Test
    void duplicate_alias_is_rejected_and_providers_are_tenant_isolated() throws Exception {
        String create = """
                {"providerKey":"GOOGLE","alias":"google","clientId":"c","clientSecret":"s"}""";
        mockMvc.perform(post("/api/v1/identity-providers").contentType(MediaType.APPLICATION_JSON)
                        .content(create).with(jwtForTenant("charlie", "admin", "idp:admin")))
                .andExpect(status().isCreated());
        // same alias in same tenant -> 409
        mockMvc.perform(post("/api/v1/identity-providers").contentType(MediaType.APPLICATION_JSON)
                        .content(create).with(jwtForTenant("charlie", "admin", "idp:admin")))
                .andExpect(status().isConflict());
        // another tenant does not see charlie's provider
        mockMvc.perform(get("/api/v1/identity-providers")
                        .with(jwtForTenant("delta", "admin", "idp:admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.alias=='google')]").doesNotExist());
    }

    @Test
    void update_toggles_enabled_and_rotates_secret_then_delete() throws Exception {
        String created = mockMvc.perform(post("/api/v1/identity-providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerKey":"GOOGLE","alias":"google","clientId":"c","clientSecret":"s"}""")
                        .with(jwtForTenant("echo", "admin", "idp:admin")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        mockMvc.perform(put("/api/v1/identity-providers/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"clientSecret\":\"rotated\"}")
                        .with(jwtForTenant("echo", "admin", "idp:admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.clientSecret").doesNotExist());

        mockMvc.perform(delete("/api/v1/identity-providers/" + id)
                        .with(jwtForTenant("echo", "admin", "idp:admin")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/identity-providers/" + id)
                        .with(jwtForTenant("echo", "admin", "idp:admin")))
                .andExpect(status().isNotFound());
    }
}
