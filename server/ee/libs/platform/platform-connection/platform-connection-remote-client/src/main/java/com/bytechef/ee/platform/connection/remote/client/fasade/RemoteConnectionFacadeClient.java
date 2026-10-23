/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.fasade;

import com.bytechef.ee.platform.connection.remote.client.service.RemoteConnectionServiceClient;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionFacadeClient implements ConnectionFacade {

    private final RemoteConnectionServiceClient remoteConnectionServiceClient;
    private final ObjectProvider<TagService> tagServiceProvider;

    @SuppressFBWarnings("EI")
    public RemoteConnectionFacadeClient(
        RemoteConnectionServiceClient remoteConnectionServiceClient, ObjectProvider<TagService> tagServiceProvider) {

        this.remoteConnectionServiceClient = remoteConnectionServiceClient;
        this.tagServiceProvider = tagServiceProvider;
    }

    @Override
    public long create(ConnectionDTO connectionDTO, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id, Runnable releaseReferences) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer executeConnectionRefresh(Long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionDTO getConnection(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectionDTO> getConnections(List<Long> connectionIds, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectionDTO> getConnections(
        String componentName, Integer connectionVersion, List<Long> connectionIds, Long tagId, Long environmentId,
        PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectionDTO> toConnectionDTOs(List<Connection> connections) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tag> getConnectionTags(PlatformType type) {
        TagService tagService = tagServiceProvider.getIfAvailable();

        if (tagService == null) {
            throw new UnsupportedOperationException("Connection tags need a TagService in this application");
        }

        List<Connection> connections = remoteConnectionServiceClient.getConnections(type);

        return tagService.getTags(
            connections.stream()
                .map(Connection::getTagIds)
                .flatMap(Collection::stream)
                .toList());
    }

    @Override
    public void update(long id, List<Tag> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(long id, String name, List<Tag> tags, int version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceAuthorizationParameters(long id, Map<String, ?> parameters) {
        throw new UnsupportedOperationException();
    }
}
