/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.ai.mcp.facade.EmbeddedMcpServerFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing embedded MCP servers.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class EmbeddedMcpServerGraphQlController {

    private final EmbeddedMcpServerFacade embeddedMcpServerFacade;

    @SuppressFBWarnings("EI")
    EmbeddedMcpServerGraphQlController(EmbeddedMcpServerFacade embeddedMcpServerFacade) {
        this.embeddedMcpServerFacade = embeddedMcpServerFacade;
    }

    @QueryMapping
    List<McpServer> embeddedMcpServers() {
        return embeddedMcpServerFacade.getEmbeddedMcpServers();
    }

    @QueryMapping
    List<Tag> embeddedMcpServerTags() {
        return embeddedMcpServerFacade.getEmbeddedMcpServerTags();
    }

    @QueryMapping
    List<ComponentDefinition> mcpComponentDefinitions() {
        return embeddedMcpServerFacade.getMcpComponentDefinitions();
    }

    @MutationMapping
    McpServer createEmbeddedMcpServer(@Argument CreateEmbeddedMcpServerInput input) {
        Environment[] environments = Environment.values();

        int environmentIndex = (int) input.environmentId();

        if (environmentIndex < 0 || environmentIndex >= environments.length) {
            throw new IllegalArgumentException("Invalid environmentId: " + input.environmentId());
        }

        return embeddedMcpServerFacade.createEmbeddedMcpServer(
            input.name(), environments[environmentIndex], input.enabled());
    }

    @MutationMapping
    boolean deleteEmbeddedMcpServer(@Argument Long mcpServerId) {
        embeddedMcpServerFacade.deleteEmbeddedMcpServer(mcpServerId);

        return true;
    }

    record CreateEmbeddedMcpServerInput(String name, long environmentId, Boolean enabled) {
    }
}
