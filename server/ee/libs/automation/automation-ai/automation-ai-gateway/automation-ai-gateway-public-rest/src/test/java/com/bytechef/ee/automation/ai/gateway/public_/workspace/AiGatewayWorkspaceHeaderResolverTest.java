/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.security.web.authentication.AiGatewayApiKeyAuthenticationToken;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiGatewayWorkspaceHeaderResolverTest {

    private static final long CALLER_USER_ID = 7L;
    private static final long CALLER_API_KEY_ID = 99L;
    private static final long CLAIMED_WORKSPACE_ID = 42L;

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final WorkspaceUserService workspaceUserService = mock(WorkspaceUserService.class);

    private final AiGatewayWorkspaceHeaderResolver resolver =
        new AiGatewayWorkspaceHeaderResolver(apiKeyService, workspaceUserService);

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void resetSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testReturnsNullWhenHeaderIsNull() {
        assertThat(resolver.resolveAndVerify(null)).isNull();

        verify(workspaceUserService, never()).fetchWorkspaceUser(org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void testReturnsNullWhenHeaderIsBlank() {
        assertThat(resolver.resolveAndVerify("   ")).isNull();

        verify(workspaceUserService, never()).fetchWorkspaceUser(org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void testReturnsNullWhenHeaderIsNotNumeric() {
        assertThat(resolver.resolveAndVerify("not-a-number")).isNull();

        verify(workspaceUserService, never()).fetchWorkspaceUser(org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void testThrowsWhenNoAuthenticationInContext() {
        assertThatThrownBy(() -> resolver.resolveAndVerify("42"))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessage("Caller is not authorized for workspace 42")
            .satisfies(exception -> {
                AiScoreWorkspaceBoundaryException boundary = (AiScoreWorkspaceBoundaryException) exception;

                assertThat(boundary.getTargetTypeCanonical())
                    .isEqualTo(AiScoreTargetType.WORKSPACE);
                assertThat(boundary.getTargetId()).isEqualTo(CLAIMED_WORKSPACE_ID);
            });
    }

    @Test
    void testThrowsWhenAuthenticationIsWrongTokenType() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            new UsernamePasswordAuthenticationToken("user", "password"));
        SecurityContextHolder.setContext(context);

        assertThatThrownBy(() -> resolver.resolveAndVerify("42"))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessage("Caller is not authorized for workspace 42");
    }

    @Test
    void testThrowsWhenAiGatewayTokenLacksApiKeyId() {
        AiGatewayApiKeyAuthenticationToken token = new AiGatewayApiKeyAuthenticationToken(
            0L, new User("user", "password", java.util.List.of()), null);

        seedSecurityContext(token);

        assertThatThrownBy(() -> resolver.resolveAndVerify("42"))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessage("Caller is not authorized for workspace 42");
    }

    @Test
    void testThrowsWhenCallerIsNotMemberOfClaimedWorkspace() {
        seedAuthenticatedCaller();

        when(workspaceUserService.fetchWorkspaceUser(CALLER_USER_ID, CLAIMED_WORKSPACE_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveAndVerify("42"))
            .isInstanceOf(AiScoreWorkspaceBoundaryException.class)
            .hasMessage("Caller is not authorized for workspace 42")
            .satisfies(exception -> {
                assertThat(exception.getMessage())
                    .doesNotContain("not a member of")
                    .doesNotContain(String.valueOf(CALLER_USER_ID));

                AiScoreWorkspaceBoundaryException boundary = (AiScoreWorkspaceBoundaryException) exception;

                assertThat(boundary.getTargetTypeCanonical())
                    .isEqualTo(AiScoreTargetType.WORKSPACE);
                assertThat(boundary.getTargetId()).isEqualTo(CLAIMED_WORKSPACE_ID);
                assertThat(boundary.getWorkspaceId()).isEqualTo(CLAIMED_WORKSPACE_ID);
            });
    }

    @Test
    void testReturnsWorkspaceIdForMember() {
        seedAuthenticatedCaller();

        WorkspaceUser membership = mock(WorkspaceUser.class);

        when(workspaceUserService.fetchWorkspaceUser(CALLER_USER_ID, CLAIMED_WORKSPACE_ID))
            .thenReturn(Optional.of(membership));

        assertThat(resolver.resolveAndVerify("42")).isEqualTo(CLAIMED_WORKSPACE_ID);
    }

    @Test
    void testTrimsWhitespaceFromHeader() {
        seedAuthenticatedCaller();

        WorkspaceUser membership = mock(WorkspaceUser.class);

        when(workspaceUserService.fetchWorkspaceUser(CALLER_USER_ID, CLAIMED_WORKSPACE_ID))
            .thenReturn(Optional.of(membership));

        assertThat(resolver.resolveAndVerify("  42  ")).isEqualTo(CLAIMED_WORKSPACE_ID);
    }

    private void seedAuthenticatedCaller() {
        AiGatewayApiKeyAuthenticationToken token = new AiGatewayApiKeyAuthenticationToken(
            0L, new User("user", "password", java.util.List.of()), CALLER_API_KEY_ID);

        seedSecurityContext(token);

        ApiKey apiKey = mock(ApiKey.class);

        when(apiKey.getUserId()).thenReturn(CALLER_USER_ID);
        when(apiKeyService.getApiKey(CALLER_API_KEY_ID)).thenReturn(apiKey);
    }

    private static void seedSecurityContext(AiGatewayApiKeyAuthenticationToken token) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        context.setAuthentication(token);

        SecurityContextHolder.setContext(context);
    }

    @SuppressWarnings("unused")
    private static void seedTokenApiKeyId(AiGatewayApiKeyAuthenticationToken token, Long apiKeyId) {
        ReflectionTestUtils.setField(token, "apiKeyId", apiKeyId);
    }
}
