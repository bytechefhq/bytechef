/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserConnectionFacade {

    long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO);

    /**
     * Returns the connections of the given connected user when the caller is that connected user or a tenant admin.
     */
    List<ConnectionDTO> getConnectedUserConnections(
        long connectedUserId, String componentName, List<Long> connectionIds);

    List<ConnectionDTO> getConnections(Long connectedUserId, String componentName, List<Long> connectionIds);
}
