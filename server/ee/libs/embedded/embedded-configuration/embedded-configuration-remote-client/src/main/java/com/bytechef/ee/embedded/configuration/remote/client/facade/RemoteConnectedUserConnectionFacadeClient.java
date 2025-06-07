/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.facade;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectedUserConnectionFacadeClient implements ConnectedUserConnectionFacade {

    @Override
    public long createConnectedUserProjectWorkflowConnection(
        long connectedUserId, String workflowReferenceCode, ConnectionDTO connectionDTO) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectionDTO> getConnections(Long connectedUserId, String componentName, List<Long> connectionIds) {
        throw new UnsupportedOperationException();
    }
}
