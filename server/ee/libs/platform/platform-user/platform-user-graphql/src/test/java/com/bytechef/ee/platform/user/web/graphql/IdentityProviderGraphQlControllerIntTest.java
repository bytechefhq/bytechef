/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.ee.platform.user.web.graphql.config.IdentityProviderGraphQlMethodSecurityTestConfiguration;
import com.bytechef.ee.platform.user.web.graphql.config.PlatformUserGraphQlTestConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    IdentityProviderGraphQlController.class, IdentityProviderGraphQlMethodSecurityTestConfiguration.class,
    PlatformUserGraphQlTestConfiguration.class
})
@GraphQlTest(
    controllers = IdentityProviderGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class IdentityProviderGraphQlControllerIntTest {

    private static final String MCP_FIELDS = """
        authoritiesClaim
        authorityMappings { authority externalGroup }
        id
        mcpAutomation
        mcpEmbedded
        mcpManagement
        validateMcpAudience
        """;

    private static final String MCP_INPUT = """
        {
            authoritiesClaim: "roles",
            authorityMappings: [{externalGroup: "sales", authority: "ROLE_SALES"}],
            clientId: "client",
            clientSecret: "secret",
            domains: ["customer.test"],
            issuerUri: "https://idp.customer.test",
            mcpAutomation: true,
            mcpEmbedded: false,
            mcpManagement: true,
            name: "Customer IdP",
            type: "OIDC",
            validateMcpAudience: true
        }
        """;

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private IdentityProviderService identityProviderService;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testCreateIdentityProviderRoundTripsTheMcpFieldsAndAuthorityMappings() {
        when(identityProviderService.create(any())).thenAnswer(invocation -> {
            IdentityProvider identityProvider = invocation.getArgument(0);

            identityProvider.setId(7L);

            return identityProvider;
        });

        graphQlTester
            .document("mutation { createIdentityProvider(input: " + MCP_INPUT + ") { " + MCP_FIELDS + " } }")
            .execute()
            .path("createIdentityProvider.authoritiesClaim")
            .entity(String.class)
            .isEqualTo("roles")
            .path("createIdentityProvider.authorityMappings[0].externalGroup")
            .entity(String.class)
            .isEqualTo("sales")
            .path("createIdentityProvider.authorityMappings[0].authority")
            .entity(String.class)
            .isEqualTo("ROLE_SALES")
            .path("createIdentityProvider.mcpAutomation")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("createIdentityProvider.mcpEmbedded")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("createIdentityProvider.mcpManagement")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("createIdentityProvider.validateMcpAudience")
            .entity(Boolean.class)
            .isEqualTo(true);

        ArgumentCaptor<IdentityProvider> identityProviderArgumentCaptor =
            ArgumentCaptor.forClass(IdentityProvider.class);

        verify(identityProviderService).create(identityProviderArgumentCaptor.capture());

        IdentityProvider identityProvider = identityProviderArgumentCaptor.getValue();

        assertThat(identityProvider.getAuthoritiesClaim()).isEqualTo("roles");
        assertThat(identityProvider.getAuthorityMappings()).isEqualTo(Map.of("sales", "ROLE_SALES"));
        assertThat(identityProvider.isMcpAutomation()).isTrue();
        assertThat(identityProvider.isMcpEmbedded()).isFalse();
        assertThat(identityProvider.isMcpManagement()).isTrue();
        assertThat(identityProvider.isValidateMcpAudience()).isTrue();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testUpdateIdentityProviderPassesTheMcpFieldsToTheService() {
        when(identityProviderService.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        graphQlTester
            .document(
                "mutation { updateIdentityProvider(id: 7, input: " + MCP_INPUT + ") { " + MCP_FIELDS + " } }")
            .execute()
            .path("updateIdentityProvider.id")
            .entity(Long.class)
            .isEqualTo(7L)
            .path("updateIdentityProvider.mcpManagement")
            .entity(Boolean.class)
            .isEqualTo(true);

        ArgumentCaptor<IdentityProvider> identityProviderArgumentCaptor =
            ArgumentCaptor.forClass(IdentityProvider.class);

        verify(identityProviderService).update(identityProviderArgumentCaptor.capture());

        IdentityProvider identityProvider = identityProviderArgumentCaptor.getValue();

        assertThat(identityProvider.getId()).isEqualTo(7L);
        assertThat(identityProvider.getAuthoritiesClaim()).isEqualTo("roles");
        assertThat(identityProvider.getAuthorityMappings()).isEqualTo(Map.of("sales", "ROLE_SALES"));
        assertThat(identityProvider.isMcpAutomation()).isTrue();
        assertThat(identityProvider.isMcpManagement()).isTrue();
        assertThat(identityProvider.isValidateMcpAudience()).isTrue();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testIdentityProvidersQueryReturnsTheMcpFields() {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setAuthoritiesClaim("roles");
        identityProvider.setAuthorityMappings(Map.of("engineering", "ROLE_ENGINEER"));
        identityProvider.setId(3L);
        identityProvider.setMcpAutomation(true);
        identityProvider.setName("Customer IdP");
        identityProvider.setType("OIDC");

        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProvider));

        graphQlTester
            .document("query { identityProviders { " + MCP_FIELDS + " } }")
            .execute()
            .path("identityProviders[0].authorityMappings[0].externalGroup")
            .entity(String.class)
            .isEqualTo("engineering")
            .path("identityProviders[0].authorityMappings[0].authority")
            .entity(String.class)
            .isEqualTo("ROLE_ENGINEER")
            .path("identityProviders[0].mcpAutomation")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("identityProviders[0].mcpManagement")
            .entity(Boolean.class)
            .isEqualTo(false);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void testNonAdminCannotCreateAnIdentityProvider() {
        assertForbidden("mutation { createIdentityProvider(input: " + MCP_INPUT + ") { id } }");
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void testNonAdminCannotUpdateAnIdentityProvider() {
        assertForbidden("mutation { updateIdentityProvider(id: 7, input: " + MCP_INPUT + ") { id } }");
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void testNonAdminCannotDeleteAnIdentityProvider() {
        assertForbidden("mutation { deleteIdentityProvider(id: 7) }");
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void testNonAdminCannotReadIdentityProviders() {
        assertForbidden("query { identityProviders { " + MCP_FIELDS + " } }");
        assertForbidden("query { identityProvider(id: 7) { " + MCP_FIELDS + " } }");
    }

    private void assertForbidden(String document) {
        graphQlTester
            .document(document)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .isNotEmpty()
                .allSatisfy(error -> assertThat(error.getErrorType()).isEqualTo(ErrorType.FORBIDDEN)));

        verifyNoInteractions(identityProviderService);
    }
}
