/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.connection.dto.ConnectionDTO;
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

    void deleteConnectedUserConnection(long connectedUserId, long connectionId);

    List<ConnectionDTO> getConnections(
        Long connectedUserId, @Nullable String componentName, List<Long> connectionIds);

    Set<Long> getOwnedConnectionIds(long connectedUserId);

    void reauthorizeConnectedUserConnection(long connectedUserId, long connectionId, Map<String, ?> parameters);

    void validateCurrentPrincipalConnectedUser(long connectedUserId);
}
