/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider.Decision;
import com.bytechef.automation.configuration.security.EmbeddedResourceResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionServiceEmbeddedScopeTest {

    private static final long USER_ID = 42L;
    private static final String EMBEDDED_VIEW = "EMBEDDED_VIEW";

    private CurrentUserResolver currentUserResolver;
    private ConnectedUserAccessDecider decider;
    private EmbeddedScopeCacheService embeddedScopeCacheService;
    private EmbeddedUserRepository embeddedUserRepository;
    private PermissionScopeRegistry permissionScopeRegistry;
    private PermissionServiceImpl permissionService;
    private ProjectRepository projectRepository;
    private WorkspaceScopeCacheService workspaceScopeCacheService;
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void setUp() {
        currentUserResolver = mock(CurrentUserResolver.class);
        decider = mock(ConnectedUserAccessDecider.class);
        embeddedScopeCacheService = mock(EmbeddedScopeCacheService.class);
        embeddedUserRepository = mock(EmbeddedUserRepository.class);
        permissionScopeRegistry = mock(PermissionScopeRegistry.class);
        projectRepository = mock(ProjectRepository.class);
        workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
        workspaceUserRepository = mock(WorkspaceUserRepository.class);

        when(decider.decideWorkspace(anyLong(), anyString())).thenReturn(Decision.NOT_GOVERNED);
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
            Map.of("connectedUserAccessDecider", decider));

        permissionService = new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository,
            workspaceScopeCacheService, workspaceUserRepository, List.of(), List.of(), permissiveResolver(), List.of(),
            beanFactory.getBeanProvider(ConnectedUserAccessDecider.class), embeddedScopeCacheService,
            embeddedUserRepository,
            new StaticListableBeanFactory().getBeanProvider(EmbeddedResourceResolver.class),
            new StaticListableBeanFactory().getBeanProvider(ConnectionService.class));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTenantAdminHoldsEveryEmbeddedScope() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "admin", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));

        when(permissionScopeRegistry.getAllScopeNames(PermissionScopeSide.EMBEDDED))
            .thenReturn(Set.of(EMBEDDED_VIEW, "INTEGRATION_CREATE"));

        assertThat(permissionService.hasEmbeddedScope("INTEGRATION_CREATE")).isTrue();
        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW, Environment.PRODUCTION)).isTrue();
        assertThat(permissionService.hasEmbeddedScopeInEveryEnvironment(EMBEDDED_VIEW)).isTrue();
        assertThat(permissionService.isEmbeddedMember()).isTrue();
        assertThat(permissionService.getMyEmbeddedScopes(null))
            .containsExactlyInAnyOrder(EMBEDDED_VIEW, "INTEGRATION_CREATE");

        verifyNoInteractions(embeddedScopeCacheService, embeddedUserRepository);
    }

    @Test
    void testMemberHoldsTheScopesTheCacheAnswers() {
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID)).thenReturn(Set.of(EMBEDDED_VIEW));

        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW)).isTrue();
        assertThat(permissionService.hasEmbeddedScope("INTEGRATION_CREATE")).isFalse();
    }

    @Test
    void testEnvironmentOverloadAsksTheCacheForThatEnvironment() {
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.STAGING))
            .thenReturn(Set.of(EMBEDDED_VIEW));

        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW, Environment.STAGING)).isTrue();
        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW, Environment.PRODUCTION)).isFalse();
        assertThat(permissionService.getMyEmbeddedScopes(Environment.STAGING))
            .containsExactly(EMBEDDED_VIEW);
    }

    @Test
    void testEveryEnvironmentRequiresAllThree() {
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of(EMBEDDED_VIEW));
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.STAGING))
            .thenReturn(Set.of(EMBEDDED_VIEW));
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.PRODUCTION))
            .thenReturn(Set.of());

        assertThat(permissionService.hasEmbeddedScopeInEveryEnvironment(EMBEDDED_VIEW)).isFalse();

        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.PRODUCTION))
            .thenReturn(Set.of(EMBEDDED_VIEW));

        assertThat(permissionService.hasEmbeddedScopeInEveryEnvironment(EMBEDDED_VIEW)).isTrue();
    }

    @Test
    void testNullEnvironmentRequiresEveryEnvironmentNotTheUnion() {
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID)).thenReturn(Set.of(EMBEDDED_VIEW));
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.DEVELOPMENT))
            .thenReturn(Set.of(EMBEDDED_VIEW));
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.STAGING)).thenReturn(Set.of());
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.PRODUCTION)).thenReturn(Set.of());

        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW, null)).isFalse();

        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.STAGING))
            .thenReturn(Set.of(EMBEDDED_VIEW));
        when(embeddedScopeCacheService.getEmbeddedScopes(USER_ID, Environment.PRODUCTION))
            .thenReturn(Set.of(EMBEDDED_VIEW));

        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW, null)).isTrue();
    }

    @Test
    void testConnectedUserNeverHoldsAnEmbeddedScope() throws Throwable {
        when(decider.decideWorkspace(anyLong(), anyString())).thenReturn(Decision.DENY);

        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW)).isFalse();
        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW, Environment.PRODUCTION)).isFalse();
        assertThat(permissionService.hasEmbeddedScopeInEveryEnvironment(EMBEDDED_VIEW)).isFalse();
        assertThat(permissionService.isEmbeddedMember()).isFalse();
        assertThat(permissionService.getMyEmbeddedScopes(null)).isEmpty();

        boolean grantedUnderSkip = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasEmbeddedScope(EMBEDDED_VIEW));

        assertThat(grantedUnderSkip).isFalse();

        verifyNoInteractions(embeddedScopeCacheService, embeddedUserRepository);
    }

    @Test
    void testAutomationAuthorizationSkipGrantsEveryEmbeddedScope() throws Throwable {
        boolean granted = AutomationAuthorizationContext.callSkippingChecks(
            () -> permissionService.hasEmbeddedScope(EMBEDDED_VIEW) &&
                permissionService.hasEmbeddedScope(EMBEDDED_VIEW, Environment.PRODUCTION) &&
                permissionService.hasEmbeddedScopeInEveryEnvironment(EMBEDDED_VIEW));

        assertThat(granted).isTrue();

        verifyNoInteractions(embeddedScopeCacheService);
    }

    @Test
    void testAnonymousHoldsNothing() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.empty());

        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW)).isFalse();
        assertThat(permissionService.hasEmbeddedScope(EMBEDDED_VIEW, Environment.DEVELOPMENT)).isFalse();
        assertThat(permissionService.hasEmbeddedScopeInEveryEnvironment(EMBEDDED_VIEW)).isFalse();
        assertThat(permissionService.isEmbeddedMember()).isFalse();
        assertThat(permissionService.getMyEmbeddedScopes(null)).isEmpty();

        verifyNoInteractions(embeddedScopeCacheService, embeddedUserRepository);
    }

    @Test
    void testIsEmbeddedMemberAsksTheMembershipRepository() {
        when(embeddedUserRepository.existsByUserId(USER_ID)).thenReturn(true);

        assertThat(permissionService.isEmbeddedMember()).isTrue();
    }

    @Test
    void testEvictionDelegatesToTheCache() {
        permissionService.evictEmbeddedScopeCache(USER_ID);
        permissionService.evictAllEmbeddedScopeCache();

        verify(embeddedScopeCacheService).evictEmbeddedScopeCache(USER_ID);
        verify(embeddedScopeCacheService).evictAllEmbeddedScopeCache();
    }

    private static ResourceVisibilityResolver permissiveResolver() {
        return (resourceType, workspaceId, candidates) -> candidates.stream()
            .map(VisibilityRecord::id)
            .collect(Collectors.toSet());
    }
}
