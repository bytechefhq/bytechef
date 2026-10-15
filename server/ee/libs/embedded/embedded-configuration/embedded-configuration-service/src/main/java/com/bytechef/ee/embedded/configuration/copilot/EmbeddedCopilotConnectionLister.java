/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.copilot;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Lists the connections an embedded connected user is entitled to use: the ones they own plus the shared ones.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedCopilotConnectionLister implements CopilotConnectionLister {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @SuppressFBWarnings("EI")
    public EmbeddedCopilotConnectionLister(ConnectedUserConnectionFacade connectedUserConnectionFacade) {
        this.connectedUserConnectionFacade = connectedUserConnectionFacade;
    }

    @Override
    public boolean supports(CopilotConnectionRequest request) {
        return request.workspaceId() == null && request.userId() == null && request.externalUserId() != null;
    }

    @Override
    public List<CopilotConnection> listConnections(CopilotConnectionRequest request) {
        Environment[] environments = Environment.values();

        if (request.environmentId() < 0 || request.environmentId() >= environments.length) {
            throw new IllegalArgumentException("Unknown environment id: " + request.environmentId());
        }

        Environment environment = environments[request.environmentId()];

        return connectedUserConnectionFacade
            .getConnectedUserConnections(
                request.externalUserId(), environment, request.componentName(), List.of())
            .stream()
            .map(connectionDTO -> new CopilotConnection(
                connectionDTO.id(), connectionDTO.name(), connectionDTO.environmentId(), connectionDTO.active()))
            .toList();
    }
}
