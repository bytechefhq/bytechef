/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserConnectionFacade {

    /**
     * Creates a connection owned by the given connected user in that connected user's environment. A requested
     * environment that differs from it is refused; a missing one falls back to it.
     */
    long createConnectedUserConnection(
        long connectedUserId, @Nullable Long requestedEnvironmentId, ConnectionDTO connectionDTO);

    /**
     * Returns the connections of the given connected user when the caller is that connected user or a tenant admin.
     */
    List<ConnectionDTO> getConnectedUserConnections(long connectedUserId, @Nullable String componentName);

    List<ConnectionDTO> getConnections(Long connectedUserId, @Nullable String componentName);
}
