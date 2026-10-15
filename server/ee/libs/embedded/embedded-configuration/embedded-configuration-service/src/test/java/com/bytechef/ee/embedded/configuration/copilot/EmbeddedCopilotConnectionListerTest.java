/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister.CopilotConnection;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister.CopilotConnectionRequest;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedCopilotConnectionListerTest {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade =
        mock(ConnectedUserConnectionFacade.class);

    private final EmbeddedCopilotConnectionLister embeddedCopilotConnectionLister =
        new EmbeddedCopilotConnectionLister(connectedUserConnectionFacade);

    @Test
    void testSupportsAnExternalUserWithoutWorkspaceOrPlatformUser() {
        assertThat(embeddedCopilotConnectionLister.supports(request(null, null, "1234567890"))).isTrue();
    }

    @Test
    void testDoesNotSupportWorkspaceOrPlatformUserRequests() {
        assertThat(embeddedCopilotConnectionLister.supports(request(7L, null, "1234567890"))).isFalse();
        assertThat(embeddedCopilotConnectionLister.supports(request(null, 42L, "1234567890"))).isFalse();
        assertThat(embeddedCopilotConnectionLister.supports(request(null, null, null))).isFalse();
    }

    @Test
    void testListsTheConnectionsTheConnectedUserIsEntitledTo() {
        ConnectionDTO connectionDTO = mock(ConnectionDTO.class);

        when(connectionDTO.id()).thenReturn(42L);
        when(connectionDTO.name()).thenReturn("My Slack");
        when(connectionDTO.environmentId()).thenReturn(0);
        when(connectionDTO.active()).thenReturn(true);

        when(connectedUserConnectionFacade.getConnectedUserConnections(
            "1234567890", Environment.DEVELOPMENT, "slack", List.of()))
                .thenReturn(List.of(connectionDTO));

        List<CopilotConnection> connections = embeddedCopilotConnectionLister.listConnections(
            request(null, null, "1234567890"));

        assertThat(connections).containsExactly(new CopilotConnection(42L, "My Slack", 0, true));
    }

    @Test
    void testRejectsAnUnknownEnvironment() {
        CopilotConnectionRequest request = new CopilotConnectionRequest(null, null, "1234567890", 99, "slack", null);

        assertThatThrownBy(() -> embeddedCopilotConnectionLister.listConnections(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("99");
    }

    private static CopilotConnectionRequest request(Long workspaceId, Long userId, String externalUserId) {
        return new CopilotConnectionRequest(
            workspaceId, userId, externalUserId, Environment.DEVELOPMENT.ordinal(), "slack", null);
    }
}
