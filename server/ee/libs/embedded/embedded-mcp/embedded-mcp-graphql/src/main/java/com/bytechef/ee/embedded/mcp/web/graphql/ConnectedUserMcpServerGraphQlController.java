/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.mcp.dto.ConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.mcp.facade.ConnectedUserMcpServerFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing MCP servers a connected user is reachable through.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
class ConnectedUserMcpServerGraphQlController {

    private final ConnectedUserMcpServerFacade connectedUserMcpServerFacade;

    @SuppressFBWarnings("EI")
    ConnectedUserMcpServerGraphQlController(ConnectedUserMcpServerFacade connectedUserMcpServerFacade) {
        this.connectedUserMcpServerFacade = connectedUserMcpServerFacade;
    }

    @QueryMapping
    List<ConnectedUserMcpServerDTO> connectedUserMcpServers(@Argument long connectedUserId) {
        return connectedUserMcpServerFacade.getConnectedUserMcpServers(connectedUserId);
    }

    @MutationMapping
    boolean enableConnectedUserMcpTool(@Argument long id, @Argument boolean enable) {
        connectedUserMcpServerFacade.enableMcpTool(id, enable);

        return true;
    }
}
