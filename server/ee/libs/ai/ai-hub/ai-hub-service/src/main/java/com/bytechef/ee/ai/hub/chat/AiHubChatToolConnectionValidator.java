/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class AiHubChatToolConnectionValidator {

    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubChatToolConnectionValidator(WorkspaceConnectionFacade workspaceConnectionFacade) {
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    public void validate(long workspaceId, String componentName, long connectionId, int environment) {
        List<ConnectionDTO> connections = workspaceConnectionFacade.getConnections(
            workspaceId, componentName, null, (long) environment, null);

        boolean accessible = connections.stream()
            .anyMatch(connection -> Objects.equals(connection.id(), connectionId));

        if (!accessible) {
            throw new IllegalArgumentException(
                "Connection " + connectionId + " is not a " + componentName +
                    " connection available in this workspace and environment");
        }
    }
}
