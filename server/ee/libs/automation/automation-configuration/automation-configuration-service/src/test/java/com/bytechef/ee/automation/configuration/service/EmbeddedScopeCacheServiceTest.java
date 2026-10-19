/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedScopeCacheServiceTest {

    private static final long USER_ID = 42L;

    private ConcurrentMapCacheManager cacheManager;
    private CustomRoleScopeResolver customRoleScopeResolver;
    private EmbeddedUserRepository embeddedUserRepository;
    private PermissionScopeRegistry permissionScopeRegistry;
    private EmbeddedScopeCacheService service;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager("embeddedScopes");
        customRoleScopeResolver = mock(CustomRoleScopeResolver.class);
        embeddedUserRepository = mock(EmbeddedUserRepository.class);
        permissionScopeRegistry = mock(PermissionScopeRegistry.class);

        service = new EmbeddedScopeCacheService(
            cacheManager, customRoleScopeResolver, permissionScopeRegistry, embeddedUserRepository);

        when(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR, PermissionScopeSide.EMBEDDED))
            .thenReturn(Set.of("WORKFLOW_EDIT"));
        when(permissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER, PermissionScopeSide.EMBEDDED))
            .thenReturn(Set.of("EMBEDDED_VIEW"));
        when(permissionScopeRegistry.getAllScopeNames(PermissionScopeSide.EMBEDDED))
            .thenReturn(Set.of("WORKFLOW_EDIT", "EMBEDDED_VIEW"));
    }

    @Test
    void testImplicitRowAnswersEveryEnvironment() {
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.empty());
        when(embeddedUserRepository.existsByUserIdAndEnvironmentIsNotNull(USER_ID))
            .thenReturn(false);
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID))
            .thenReturn(Optional.of(EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR)));

        assertThat(service.getEmbeddedScopes(USER_ID, Environment.PRODUCTION)).contains("WORKFLOW_EDIT");
    }

    @Test
    void testExplicitModeDeniesAnEnvironmentWithoutARow() {
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.empty());
        when(embeddedUserRepository.existsByUserIdAndEnvironmentIsNotNull(USER_ID))
            .thenReturn(true);
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID))
            .thenReturn(Optional.of(EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR)));

        assertThat(service.getEmbeddedScopes(USER_ID, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testExplicitModeUnionsForTheUnawareAnswer() {
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID))
            .thenReturn(Optional.empty());
        when(embeddedUserRepository.findAllByUserId(USER_ID))
            .thenReturn(
                List.of(
                    EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT),
                    EmbeddedUser.forRole(USER_ID, WorkspaceRole.VIEWER, Environment.PRODUCTION)));

        assertThat(service.getEmbeddedScopes(USER_ID)).contains("WORKFLOW_EDIT", "EMBEDDED_VIEW");
    }

    @Test
    void testNoRowsMeansNoScopes() {
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID))
            .thenReturn(Optional.empty());
        when(embeddedUserRepository.findAllByUserId(USER_ID))
            .thenReturn(List.of());

        assertThat(service.getEmbeddedScopes(USER_ID)).isEmpty();
        assertThat(service.getEmbeddedScopes(USER_ID, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testCustomRoleScopesAreIntersectedWithTheEmbeddedCatalogue() {
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID))
            .thenReturn(Optional.of(EmbeddedUser.forCustomRole(USER_ID, 7L)));
        when(customRoleScopeResolver.resolveScopes(7L))
            .thenReturn(Optional.of(Set.of("EMBEDDED_VIEW", "PROJECT_DELETE")));

        assertThat(service.getEmbeddedScopes(USER_ID)).containsExactly("EMBEDDED_VIEW");
    }

    @Test
    void testEvictionClearsEveryEnvironmentKey() {
        Cache cache = Objects.requireNonNull(cacheManager.getCache("embeddedScopes"));

        cache.put(TenantCacheKeyUtils.getKey(USER_ID), Set.of("WORKFLOW_EDIT"));

        for (Environment environment : Environment.values()) {
            cache.put(TenantCacheKeyUtils.getKey(USER_ID, environment), Set.of("WORKFLOW_EDIT"));
        }

        service.evictEmbeddedScopeCache(USER_ID);

        assertThat(cache.get(TenantCacheKeyUtils.getKey(USER_ID))).isNull();

        for (Environment environment : Environment.values()) {
            assertThat(cache.get(TenantCacheKeyUtils.getKey(USER_ID, environment))).isNull();
        }
    }
}
