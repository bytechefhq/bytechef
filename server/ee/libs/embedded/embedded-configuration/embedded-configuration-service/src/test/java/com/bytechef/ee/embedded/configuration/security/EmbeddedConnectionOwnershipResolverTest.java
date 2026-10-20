/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedConnectionOwnershipResolverTest {

    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final EmbeddedConnectionOwnershipResolver resolver =
        new EmbeddedConnectionOwnershipResolver(connectionService);

    private void stubConnection(PlatformType type) {
        Connection connection = new Connection();

        connection.setType(type);

        when(connectionService.fetchConnection(7L)).thenReturn(Optional.of(connection));
    }

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("Connection");
    }

    @Test
    void testResolvesAnEmbeddedRow() {
        stubConnection(PlatformType.EMBEDDED);

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(resolver.resolveOwner(Long.valueOf(7L))).isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testAMissingRowIsUnknown() {
        when(connectionService.fetchConnection(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testAnotherSidesRowIsUnknown() {
        stubConnection(PlatformType.AUTOMATION);

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }
}
