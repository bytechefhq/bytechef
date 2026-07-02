/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Lists an embedded connected user's connections for a component, sourced from their IntegrationInstances. Serves the
 * copilot {@code listConnectionsForComponent} tool when invoked from the embedded workflow chat, which has no workspace
 * context; the in-editor / AI Hub surfaces use the workspace path instead.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedCopilotConnectionLister implements CopilotConnectionLister {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade;
    private final ConnectedUserService connectedUserService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public EmbeddedCopilotConnectionLister(
        ConnectedUserConnectionFacade connectedUserConnectionFacade, ConnectedUserService connectedUserService,
        EnvironmentService environmentService) {

        this.connectedUserConnectionFacade = connectedUserConnectionFacade;
        this.connectedUserService = connectedUserService;
        this.environmentService = environmentService;
    }

    @Override
    public boolean supports(CopilotConnectionRequest request) {
        return request.workspaceId() == null && request.externalUserId() != null;
    }

    @Override
    public List<CopilotConnection> listConnections(CopilotConnectionRequest request) {
        Environment environment = environmentService.getEnvironment(request.environmentId());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(request.externalUserId(), environment);

        List<ConnectionDTO> connectionDTOs =
            connectedUserConnectionFacade.getConnections(connectedUser.getId(), request.componentName(), List.of());

        return connectionDTOs.stream()
            .map(connectionDTO -> new CopilotConnection(
                connectionDTO.id(), connectionDTO.name(), connectionDTO.environmentId(), connectionDTO.active()))
            .toList();
    }
}
