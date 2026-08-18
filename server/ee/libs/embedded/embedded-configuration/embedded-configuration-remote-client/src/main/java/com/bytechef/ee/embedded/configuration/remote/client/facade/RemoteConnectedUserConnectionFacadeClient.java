/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserConnectionFacadeClient implements ConnectedUserConnectionFacade {

    @Override
    public long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteConnectedUserConnection(long connectedUserId, long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectionDTO> getConnections(
        Long connectedUserId, @Nullable String componentName, List<Long> connectionIds) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void reauthorizeConnectedUserConnection(long connectedUserId, long connectionId, Map<String, ?> parameters) {
        throw new UnsupportedOperationException();
    }
}
