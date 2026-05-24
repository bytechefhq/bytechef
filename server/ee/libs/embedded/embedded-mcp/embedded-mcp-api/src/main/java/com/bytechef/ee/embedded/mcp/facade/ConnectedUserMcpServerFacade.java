/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.mcp.dto.ConnectedUserMcpServerDTO;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserMcpServerFacade {

    void enableMcpTool(long mcpIntegrationInstanceToolId, boolean enable);

    List<ConnectedUserMcpServerDTO> getConnectedUserMcpServers(long connectedUserId);
}
