/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.fasade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.connection.remote.client.service.RemoteConnectionServiceClient;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RemoteConnectionFacadeClientTest {

    private RemoteConnectionServiceClient remoteConnectionServiceClient;
    private StaticListableBeanFactory tagServiceBeanFactory;
    private RemoteConnectionFacadeClient remoteConnectionFacadeClient;

    @BeforeEach
    void beforeEach() {
        remoteConnectionServiceClient = mock(RemoteConnectionServiceClient.class);
        tagServiceBeanFactory = new StaticListableBeanFactory();

        remoteConnectionFacadeClient = new RemoteConnectionFacadeClient(
            remoteConnectionServiceClient, tagServiceBeanFactory.getBeanProvider(TagService.class));
    }

    @Test
    void testGetConnectionTagsReadsTheTagsOfTheRemoteConnections() {
        TagService tagService = mock(TagService.class);
        Tag tag = new Tag(3L, "billing");

        tagServiceBeanFactory.addBean("tagService", tagService);

        when(remoteConnectionServiceClient.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of(connection(List.of(3L)), connection(List.of(4L))));
        when(tagService.getTags(List.of(3L, 4L)))
            .thenReturn(List.of(tag));

        assertThat(remoteConnectionFacadeClient.getConnectionTags(PlatformType.EMBEDDED)).containsExactly(tag);
    }

    @Test
    void testGetConnectionTagsWithoutATagServiceIsUnsupported() {
        assertThatThrownBy(() -> remoteConnectionFacadeClient.getConnectionTags(PlatformType.EMBEDDED))
            .isInstanceOf(UnsupportedOperationException.class);

        verifyNoInteractions(remoteConnectionServiceClient);
    }

    private static Connection connection(List<Long> tagIds) {
        Connection connection = new Connection();

        connection.setTagIds(tagIds);

        return connection;
    }
}
