/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.security.ConnectionOwnershipExemption;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionAdminFacadeTest {

    private ConnectionAdminFacadeImpl connectionAdminFacade;
    private ConnectionService connectionService;
    private PermissionService permissionService;
    private TagService tagService;

    @BeforeEach
    void setUp() {
        connectionService = mock(ConnectionService.class);
        permissionService = mock(PermissionService.class);
        tagService = mock(TagService.class);

        connectionAdminFacade = new ConnectionAdminFacadeImpl(
            mock(ConnectedUserConnectionService.class), mock(ConnectionFacade.class),
            mock(ConnectionOwnershipExemption.class), connectionService, permissionService, tagService);

        when(connectionService.getConnections(PlatformType.EMBEDDED)).thenReturn(
            List.of(
                connection(Environment.DEVELOPMENT, 1L), connection(Environment.STAGING, 2L),
                connection(Environment.PRODUCTION, 3L)));
        when(tagService.getTags(anyList())).thenReturn(List.of(new Tag("tag")));
    }

    @Test
    void testGetConnectionTagsReadsOnlyEnvironmentsTheCallerCanView() {
        when(permissionService.hasEmbeddedScope("CONNECTION_VIEW", Environment.PRODUCTION)).thenReturn(true);

        connectionAdminFacade.getConnectionTags();

        verify(tagService).getTags(List.of(3L));
    }

    @Test
    void testGetConnectionTagsReadsEveryEnvironmentForAnEmbeddedWideViewer() {
        for (Environment environment : Environment.values()) {
            when(permissionService.hasEmbeddedScope("CONNECTION_VIEW", environment)).thenReturn(true);
        }

        assertThat(connectionAdminFacade.getConnectionTags()).hasSize(1);

        verify(tagService).getTags(List.of(1L, 2L, 3L));
    }

    @Test
    void testGetConnectionTagsReadsNothingWithoutTheScope() {
        connectionAdminFacade.getConnectionTags();

        verify(tagService).getTags(List.of());
    }

    private static Connection connection(Environment environment, long tagId) {
        Connection connection = new Connection();

        connection.setEnvironmentId(environment.ordinal());
        connection.setTagIds(List.of(tagId));
        connection.setType(PlatformType.EMBEDDED);

        return connection;
    }
}
