/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @version ee
 */
@AutoConfigureMockMvc
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
@SpringBootTest(classes = ExecutionApplication.class)
class ExecutionSessionIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    @Test
    void testUnauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/platform/internal/jobs"))
            .andExpect(result -> assertThat(result.getResponse()
                .getStatus()).isEqualTo(401));
    }

    @Test
    void testSessionWrittenElsewhereIsAccepted() throws Exception {
        String sessionId = saveSession(sessionRepository, formLoginSecurityContext());

        mockMvc.perform(get("/api/platform/internal/jobs")
            .cookie(new Cookie("SESSION", EncodingUtils.base64EncodeToString(sessionId))))
            .andExpect(result -> assertThat(result.getResponse()
                .getStatus()).isEqualTo(200));
    }

    @Test
    void testSamlSessionWrittenElsewhereIsAccepted() throws Exception {
        String sessionId = saveSession(sessionRepository, samlLoginSecurityContext());

        mockMvc.perform(get("/api/platform/internal/jobs")
            .cookie(new Cookie("SESSION", EncodingUtils.base64EncodeToString(sessionId))))
            .andExpect(result -> assertThat(result.getResponse()
                .getStatus()).isEqualTo(200));
    }

    @Test
    void testPublicFormsAreOpen() throws Exception {
        for (String path : List.of(
            "/api/automation/internal/trigger-form/abc", "/api/platform/internal/approval-form/abc")) {

            assertThat(anonymousStatus(get(path))).as(path)
                .isNotIn(401, 403);
        }
    }

    private int anonymousStatus(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        try {
            return mockMvc.perform(requestBuilder)
                .andReturn()
                .getResponse()
                .getStatus();
        } catch (ServletException servletException) {
            return 500;
        }
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
