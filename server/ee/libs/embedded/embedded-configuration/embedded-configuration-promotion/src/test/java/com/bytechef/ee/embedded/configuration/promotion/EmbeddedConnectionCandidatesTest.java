/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.promotion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.promotion.dto.PromotionConnectionCandidate;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class EmbeddedConnectionCandidatesTest {

    @Mock
    private ConnectedUserConnectionMembership connectedUserConnectionMembership;

    @Mock
    private ConnectionService connectionService;

    private EmbeddedConnectionCandidates embeddedConnectionCandidates;

    @BeforeEach
    void setUp() {
        embeddedConnectionCandidates = new EmbeddedConnectionCandidates(
            connectedUserConnectionMembership, connectionService);
    }

    @Test
    void testCandidatesExcludeConnectedUserOwnedConnections() {
        Connection administratorConnection = connection(1L, "Zeta");
        Connection ownedConnection = connection(2L, "Alpha");

        when(connectionService.getConnections(
            eq("slack"), eq(null), eq(null), eq((long) Environment.PRODUCTION.ordinal()),
            eq(PlatformType.EMBEDDED))).thenReturn(List.of(administratorConnection, ownedConnection));
        when(connectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(any())).thenReturn(Set.of(2L));

        List<PromotionConnectionCandidate> candidates = embeddedConnectionCandidates.candidates(
            "slack", Environment.PRODUCTION);

        assertThat(candidates).containsExactly(new PromotionConnectionCandidate(1L, "Zeta"));
    }

    @Test
    void testCandidatesAreOrderedByNameThenId() {
        List<Connection> connections = List.of(connection(5L, "Beta"), connection(4L, "Beta"), connection(9L, "Alpha"));

        when(connectionService.getConnections(
            eq("slack"), eq(null), eq(null), eq((long) Environment.STAGING.ordinal()),
            eq(PlatformType.EMBEDDED))).thenReturn(connections);
        when(connectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(any())).thenReturn(Set.of());

        List<PromotionConnectionCandidate> candidates = embeddedConnectionCandidates.candidates(
            "slack", Environment.STAGING);

        assertThat(candidates).containsExactly(
            new PromotionConnectionCandidate(9L, "Alpha"), new PromotionConnectionCandidate(4L, "Beta"),
            new PromotionConnectionCandidate(5L, "Beta"));
    }

    @Test
    void testCandidatesAreEmptyWhenNoEmbeddedConnectionExists() {
        when(connectionService.getConnections(
            eq("slack"), eq(null), eq(null), eq((long) Environment.PRODUCTION.ordinal()),
            eq(PlatformType.EMBEDDED))).thenReturn(List.of());
        when(connectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(any())).thenReturn(Set.of());

        assertThat(embeddedConnectionCandidates.candidates("slack", Environment.PRODUCTION)).isEmpty();
    }

    private static Connection connection(long id, String name) {
        return Connection.builder()
            .id(id)
            .name(name)
            .type(PlatformType.EMBEDDED)
            .build();
    }
}
