/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.ConnectionOrigin;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.tag.domain.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserConnectionFacade {

    long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO);

    long createConnectedUserConnectionAsAdmin(long connectedUserId, ConnectionDTO connectionDTO);

    void deleteConnectedUserConnection(long connectedUserId, long connectionId);

    List<Tag> getConnectedUserConnectionTags(long connectedUserId);

    List<ConnectionDTO> getConnectedUserConnections(
        long connectedUserId, @Nullable String componentName, List<Long> connectionIds);

    List<ConnectionDTO> getConnectedUserConnectionsForAdmin(
        long connectedUserId, @Nullable String componentName, List<Long> connectionIds);

    List<ConnectionDTO> getConnectedUserConnections(
        String externalUserId, Environment environment, @Nullable String componentName, List<Long> connectionIds);

    List<ConnectionDTO> getConnections(
        Long connectedUserId, @Nullable String componentName, List<Long> connectionIds);

    Map<Long, ConnectionOrigin> getConnectionOrigins(long connectedUserId, Collection<Long> connectionIds);

    Set<Long> getOwnedConnectionIds(long connectedUserId);

    Set<Long> getOwnedConnectionIds(String externalUserId, Environment environment);

    Set<Long> getSharedConnectionIds();

    void reauthorizeConnectedUserConnection(long connectedUserId, long connectionId, Map<String, ?> parameters);

    void updateConnectedUserConnectionTags(long connectedUserId, long connectionId, List<Tag> tags);

    void updateIntegrationInstanceConnectionCredentials(
        long connectedUserId, long connectionId, Map<String, ?> parameters, int version);
}
