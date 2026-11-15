/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceConnectionGrantContributorTest {

    private static final long CONNECTION_ID = 10L;

    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final ResourceGrantService resourceGrantService = mock(ResourceGrantService.class);
    private final WorkspaceConnectionGrantContributor workspaceConnectionGrantContributor =
        new WorkspaceConnectionGrantContributor(connectionService, resourceGrantService);

    @Test
    void testOrganizationConnectionsAreSharedWithEveryWorkspace() {
        Connection organizationConnection = new Connection();

        organizationConnection.setId(20L);

        when(connectionService.getConnectionsByVisibility(ResourceVisibility.ORGANIZATION, PlatformType.AUTOMATION))
            .thenReturn(List.of(organizationConnection));

        assertThat(workspaceConnectionGrantContributor.getSharedConnectionIds()).containsExactly(20L);
    }

    @Test
    void testDeletingAConnectionDeletesItsGrants() {
        workspaceConnectionGrantContributor.beforeDelete(CONNECTION_ID);

        verify(resourceGrantService).deleteGrants("Connection", CONNECTION_ID);
    }
}
