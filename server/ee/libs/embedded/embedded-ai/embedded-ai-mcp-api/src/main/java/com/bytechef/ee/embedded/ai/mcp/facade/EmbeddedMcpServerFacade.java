/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedMcpServerFacade {

    McpServer createEmbeddedMcpServer(String name, Environment environment, boolean enabled);

    void deleteEmbeddedMcpServer(long mcpServerId);

    List<McpServer> getEmbeddedMcpServers();

    List<Tag> getEmbeddedMcpServerTags();

    List<ComponentDefinition> getMcpComponentDefinitions();
}
