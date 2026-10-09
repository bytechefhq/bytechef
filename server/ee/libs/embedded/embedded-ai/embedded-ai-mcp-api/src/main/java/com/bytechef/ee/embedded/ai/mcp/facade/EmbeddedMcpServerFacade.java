/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedMcpServerFacade {

    McpComponent createEmbeddedMcpComponent(McpComponent mcpComponent, List<McpTool> mcpTools);

    McpServer createEmbeddedMcpServer(String name, Environment environment, Boolean enabled);

    void deleteEmbeddedMcpComponent(long mcpComponentId);

    void deleteEmbeddedMcpServer(long mcpServerId);

    void deleteEmbeddedMcpTool(long mcpToolId);

    List<McpServer> getEmbeddedMcpServers();

    List<Tag> getEmbeddedMcpServerTags();

    List<ComponentDefinition> getMcpComponentDefinitions();

    McpComponent updateEmbeddedMcpComponent(McpComponent mcpComponent, List<McpTool> mcpTools);

    McpServer updateEmbeddedMcpServer(long mcpServerId, String name, Boolean enabled);

    McpServer updateEmbeddedMcpServerSecretKey(long mcpServerId);

    List<Tag> updateEmbeddedMcpServerTags(long mcpServerId, List<Tag> tags);

    McpTool updateEmbeddedMcpTool(McpTool mcpTool);

    McpTool updateEmbeddedMcpToolEnabled(long mcpToolId, boolean enabled);
}
