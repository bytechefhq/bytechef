/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test class for {@link ResourceVisibilityResolverImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceVisibilityResolverImplTest {

    private static final String CONNECTION = "Connection";
    private static final long CURRENT_USER_ID = 7L;
    private static final long WORKSPACE_ID = 1L;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @Mock
    private ResourceGrantService resourceGrantService;

    private ResourceVisibilityResolverImpl resolver;

    @BeforeEach
    void setUp() {
        resolver = new ResourceVisibilityResolverImpl(currentUserResolver, resourceGrantService);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(CURRENT_USER_ID));

        authenticate("ana");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(String login, String... authorities) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    login, "password", List.of(authorities)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()));
    }

    @Test
    void testGrantMakesPrivateResourceVisible() {
        when(resourceGrantService.filterGrantedResourceIds(anyString(), anyLong(), any())).thenReturn(Set.of(10L));

        Set<Long> visibleIds = resolver.filterVisibleIds(
            CONNECTION, WORKSPACE_ID, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visibleIds).containsExactly(10L);
    }

    @Test
    void testPrivateWithoutGrantStaysHidden() {
        when(resourceGrantService.filterGrantedResourceIds(anyString(), anyLong(), any())).thenReturn(Set.of());

        Set<Long> visibleIds = resolver.filterVisibleIds(
            CONNECTION, WORKSPACE_ID, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visibleIds).isEmpty();
    }

    @Test
    void testNoGrantQueryWhenNothingIsPrivate() {
        Set<Long> visibleIds = resolver.filterVisibleIds(
            CONNECTION, WORKSPACE_ID,
            List.of(
                new VisibilityRecord(10L, ResourceVisibility.WORKSPACE, "ivica"),
                new VisibilityRecord(11L, ResourceVisibility.ORGANIZATION, "ivica")));

        assertThat(visibleIds).containsExactlyInAnyOrder(10L, 11L);

        // A database round-trip on a path with nothing to ask about is pure waste; the common case is a list
        // where every row is workspace-visible.
        verify(resourceGrantService, never()).filterGrantedResourceIds(anyString(), anyLong(), any());
    }

    @Test
    void testGrantQueryIsIssuedOnceForManyPrivateCandidates() {
        when(resourceGrantService.filterGrantedResourceIds(anyString(), anyLong(), any())).thenReturn(Set.of(10L));

        resolver.filterVisibleIds(
            CONNECTION, WORKSPACE_ID,
            List.of(
                new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica"),
                new VisibilityRecord(11L, ResourceVisibility.PRIVATE, "ivica"),
                new VisibilityRecord(12L, ResourceVisibility.PRIVATE, "ivica")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<Long>> captor = ArgumentCaptor.forClass(java.util.Collection.class);

        verify(resourceGrantService).filterGrantedResourceIds(eq(CONNECTION), eq(CURRENT_USER_ID), captor.capture());

        // One query carrying all three ids, not three queries.
        assertThat(captor.getValue()).containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    void testOnlyUndecidedCandidatesReachTheGrantQuery() {
        when(resourceGrantService.filterGrantedResourceIds(anyString(), anyLong(), any())).thenReturn(Set.of());

        resolver.filterVisibleIds(
            CONNECTION, WORKSPACE_ID,
            List.of(
                new VisibilityRecord(10L, ResourceVisibility.WORKSPACE, "ivica"),
                new VisibilityRecord(11L, ResourceVisibility.PRIVATE, "ana"),
                new VisibilityRecord(12L, ResourceVisibility.PRIVATE, "ivica")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<Long>> captor = ArgumentCaptor.forClass(java.util.Collection.class);

        verify(resourceGrantService).filterGrantedResourceIds(anyString(), anyLong(), captor.capture());

        // 10 is workspace-visible and 11 is ana's own, so only 12 is still undecided.
        assertThat(captor.getValue()).containsExactly(12L);
    }

    @Test
    void testOwnerSeesPrivateResourceWithoutGrant() {
        authenticate("ivica");

        when(resourceGrantService.filterGrantedResourceIds(anyString(), anyLong(), any())).thenReturn(Set.of());

        Set<Long> visibleIds = resolver.filterVisibleIds(
            CONNECTION, WORKSPACE_ID, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visibleIds).containsExactly(10L);
    }

    @Test
    void testAdminSeesEverythingWithoutGrantQuery() {
        authenticate("marko", AuthorityConstants.ADMIN);

        Set<Long> visibleIds = resolver.filterVisibleIds(
            CONNECTION, WORKSPACE_ID, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        assertThat(visibleIds).containsExactly(10L);

        verify(resourceGrantService, never()).filterGrantedResourceIds(anyString(), anyLong(), any());
    }

    @Test
    void testUnresolvableCurrentUserHidesPrivateResources() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.empty());

        Set<Long> visibleIds = resolver.filterVisibleIds(
            CONNECTION, WORKSPACE_ID, List.of(new VisibilityRecord(10L, ResourceVisibility.PRIVATE, "ivica")));

        // Fail closed: without a user id there is no grant to look up, and guessing would widen access.
        assertThat(visibleIds).isEmpty();

        verify(resourceGrantService, never()).filterGrantedResourceIds(anyString(), anyLong(), any());
    }

    @Test
    void testEmptyCandidateSetReturnsEmpty() {
        assertThat(resolver.filterVisibleIds(CONNECTION, WORKSPACE_ID, List.of())).isEmpty();

        verify(resourceGrantService, never()).filterGrantedResourceIds(anyString(), anyLong(), any());
    }
}
