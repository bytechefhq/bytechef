/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
@SpringBootTest(
    classes = ConfigurationApplication.class,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true",
        "bytechef.oauth2.resource-server.issuers[0].self=true",
        "bytechef.oauth2.resource-server.issuers[0].tenant-claim=tenant_id",
        "bytechef.oauth2.resource-server.issuers[0].uri=" + ConfigurationOauth2AuthorizationServerIntTest.PUBLIC_URL,
        "bytechef.public-url=" + ConfigurationOauth2AuthorizationServerIntTest.PUBLIC_URL
    })
class ConfigurationOauth2AuthorizationServerIntTest {

    static final String PUBLIC_URL = "https://bytechef.example.test";

    private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    private static final String REDIRECT_URI = "http://127.0.0.1/callback";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    @Test
    void testGrpcServerDoesNotRequireABearerTokenFromOtherApps() {
        assertThat(applicationContext.getBeanNamesForType(AuthenticationProcessInterceptor.class)).isEmpty();
    }

    @Test
    void testClientAndKeyTablesExist() {
        assertThat(
            jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_name IN " +
                    "('oauth2_registered_client', 'oauth2_jwk')",
                String.class))
                    .containsExactlyInAnyOrder("oauth2_registered_client", "oauth2_jwk");
    }

    @Test
    void testAuthorizationServerMetadataNamesThePublicIssuer() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/.well-known/oauth-authorization-server"))
            .andReturn()
            .getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(readMap(response).get("issuer")).isEqualTo(PUBLIC_URL);
    }

    @Test
    void testDynamicClientRegistrationGrantsTheMcpScopes() throws Exception {
        Map<String, Object> registeredClient = registerClient();

        assertThat(String.valueOf(registeredClient.get("scope"))
            .split(" ")).containsExactlyInAnyOrder("mcp:automation", "mcp:management");
    }

    @Test
    void testAuthorizeWithoutSessionRedirectsToTheLoginPage() throws Exception {
        String clientId = String.valueOf(registerClient().get("client_id"));

        MockHttpServletResponse response = mockMvc.perform(get(URI.create(authorizeUri(clientId)))
            .accept(MediaType.TEXT_HTML))
            .andReturn()
            .getResponse();

        assertThat(response.getStatus()).as(response.getContentAsString())
            .isEqualTo(302);
        assertThat(response.getHeader(HttpHeaders.LOCATION)).startsWith("/login?redirect=");
    }

    @Test
    void testAuthorizeWithAGatewaySessionRedirectsToTheConsentPageOnThePublicUrl() throws Exception {
        String clientId = String.valueOf(registerClient().get("client_id"));
        String sessionId = saveSession(sessionRepository, formLoginSecurityContext());

        MockHttpServletResponse response = mockMvc.perform(get(URI.create(authorizeUri(clientId)))
            .accept(MediaType.TEXT_HTML)
            .cookie(new Cookie("SESSION", EncodingUtils.base64EncodeToString(sessionId))))
            .andReturn()
            .getResponse();

        assertThat(response.getStatus()).as(response.getContentAsString())
            .isEqualTo(302);
        assertThat(response.getHeader(HttpHeaders.LOCATION)).startsWith(PUBLIC_URL + "/oauth2/consent?");
    }

    private static String authorizeUri(String clientId) {
        return "/oauth2/authorize?response_type=code&client_id=" + clientId + "&redirect_uri=" +
            URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) + "&scope=mcp:management&state=state-1" +
            "&code_challenge=" + CODE_CHALLENGE + "&code_challenge_method=S256&resource=" +
            URLEncoder.encode(PUBLIC_URL + "/api/management/mcp", StandardCharsets.UTF_8);
    }

    private static SecurityContext formLoginSecurityContext() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        return new SecurityContextImpl(
            UsernamePasswordAuthenticationToken.authenticated(
                new User("admin@localhost.com", "", authorities), null, authorities));
    }

    private static <S extends Session> String saveSession(
        SessionRepository<S> sessionRepository, SecurityContext securityContext) {

        S session = sessionRepository.createSession();

        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

        sessionRepository.save(session);

        return session.getId();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(MockHttpServletResponse response) throws Exception {
        return objectMapper.readValue(response.getContentAsString(), Map.class);
    }

    private Map<String, Object> registerClient() throws Exception {
        String body = objectMapper.writeValueAsString(
            Map.of(
                "client_name", "MCP test client", "grant_types", List.of("authorization_code", "refresh_token"),
                "redirect_uris", List.of(REDIRECT_URI), "response_types", List.of("code"),
                "scope", "mcp:automation mcp:management", "token_endpoint_auth_method", "none"));

        MockHttpServletResponse response = mockMvc.perform(post("/oauth2/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andReturn()
            .getResponse();

        assertThat(response.getStatus()).as(response.getContentAsString())
            .isEqualTo(201);

        return readMap(response);
    }
}
