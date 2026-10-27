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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertion;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 */
@AutoConfigureMockMvc
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
@SpringBootTest(classes = ConfigurationApplication.class)
class ConfigurationSessionIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    @Test
    void testUnauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/automation/internal/projects"))
            .andExpect(result -> assertThat(result.getResponse()
                .getStatus()).isEqualTo(401));
    }

    @Test
    void testEmbeddedPagesAreDenied() throws Exception {
        mockMvc.perform(get("/workflow-builder.html"))
            .andExpect(result -> assertThat(result.getResponse()
                .getStatus()).isEqualTo(403));
    }

    @Test
    void testSessionWrittenElsewhereIsAccepted() throws Exception {
        String sessionId = saveSession(sessionRepository, formLoginSecurityContext());

        mockMvc.perform(get("/api/automation/internal/workspaces/1/projects")
            .cookie(new Cookie("SESSION", EncodingUtils.base64EncodeToString(sessionId))))
            .andExpect(result -> assertThat(result.getResponse()
                .getStatus()).isEqualTo(200));
    }

    @Test
    void testSamlSessionWrittenElsewhereIsAccepted() throws Exception {
        String sessionId = saveSession(sessionRepository, samlLoginSecurityContext());

        mockMvc.perform(get("/api/automation/internal/workspaces/1/projects")
            .cookie(new Cookie("SESSION", EncodingUtils.base64EncodeToString(sessionId))))
            .andExpect(result -> assertThat(result.getResponse()
                .getStatus()).isEqualTo(200));
    }

    @Test
    void testWebhookTestTriggerReachesItsController() throws Exception {
        MockHttpServletResponse response;

        try {
            response = mockMvc.perform(post("/webhooks/abc/test/environments/1"))
                .andReturn()
                .getResponse();
        } catch (ServletException servletException) {
            return;
        }

        assertThat(response.getForwardedUrl()).isNull();
        assertThat(response.getStatus()).isNotIn(401, 403);
    }

    private static SecurityContext formLoginSecurityContext() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        return new SecurityContextImpl(
            UsernamePasswordAuthenticationToken.authenticated(
                new User("admin@localhost.com", "", authorities), null, authorities));
    }

    @SuppressWarnings("deprecation")
    private static SecurityContext samlLoginSecurityContext() {
        Saml2ResponseAssertion saml2ResponseAssertion = Saml2ResponseAssertion.withResponseValue("<saml2:Response/>")
            .nameId("admin@localhost.com")
            .attributes(Map.of("email", List.of("admin@localhost.com")))
            .build();

        return new SecurityContextImpl(
            new Saml2AssertionAuthentication(
                new DefaultSaml2AuthenticatedPrincipal("admin@localhost.com", saml2ResponseAssertion),
                saml2ResponseAssertion,
                List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.SAML_RESPONSE_AUTHORITY)),
                "saml-1"));
    }

    private static <S extends Session> String saveSession(
        SessionRepository<S> sessionRepository, SecurityContext securityContext) {

        S session = sessionRepository.createSession();

        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

        sessionRepository.save(session);

        return session.getId();
    }
}
