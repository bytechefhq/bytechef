/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.ai.mcp.facade.EmbeddedMcpServerFacade;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
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
    List<McpComponent> embeddedMcpComponentsByServerId(@Argument long mcpServerId) {
        return embeddedMcpServerFacade.getEmbeddedMcpServerMcpComponents(mcpServerId);
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
    List<McpTool> embeddedMcpToolsByComponentId(@Argument long mcpComponentId) {
        return embeddedMcpServerFacade.getEmbeddedMcpComponentMcpTools(mcpComponentId);
    }

    @QueryMapping
    List<ComponentDefinition> mcpComponentDefinitions() {
        return embeddedMcpServerFacade.getMcpComponentDefinitions();
    }

    @MutationMapping
    McpComponent createEmbeddedMcpComponent(@Argument McpComponentWithToolsInput input) {
        McpComponent mcpComponent = new McpComponent(
            input.componentName(), input.componentVersion(), input.mcpServerId(), input.connectionId());

        if (input.requiredAuthorities() != null) {
            mcpComponent.setRequiredAuthorities(new HashSet<>(input.requiredAuthorities()));
        }

        return embeddedMcpServerFacade.createEmbeddedMcpComponent(mcpComponent, toMcpTools(input.tools()));
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
    boolean deleteEmbeddedMcpComponent(@Argument long id) {
        embeddedMcpServerFacade.deleteEmbeddedMcpComponent(id);

        return true;
    }

    @MutationMapping
    boolean deleteEmbeddedMcpServer(@Argument Long mcpServerId) {
        embeddedMcpServerFacade.deleteEmbeddedMcpServer(mcpServerId);

        return true;
    }

    @MutationMapping
    boolean deleteEmbeddedMcpTool(@Argument long id) {
        embeddedMcpServerFacade.deleteEmbeddedMcpTool(id);

        return true;
    }

    @MutationMapping
    McpComponent updateEmbeddedMcpComponent(@Argument long id, @Argument McpComponentWithToolsInput input) {
        if (input.version() == null) {
            throw new GraphQlBadRequestException(
                "MISSING_FIELD", "version is required to update MCP component " + id, Map.of("field", "version"));
        }

        McpComponent mcpComponent = new McpComponent(
            input.componentName(), input.componentVersion(), input.mcpServerId(), input.connectionId(),
            input.version());

        mcpComponent.setId(id);

        if (input.requiredAuthorities() != null) {
            mcpComponent.setRequiredAuthorities(new HashSet<>(input.requiredAuthorities()));
        }

        return embeddedMcpServerFacade.updateEmbeddedMcpComponent(mcpComponent, toMcpTools(input.tools()));
    }

    @MutationMapping
    McpServer updateEmbeddedMcpServer(@Argument long id, @Argument McpServerUpdateInput input) {
        return embeddedMcpServerFacade.updateEmbeddedMcpServer(
            id, input.name(), input.enabled(), input.enforceToolAuthorization(), input.authenticationRequired());
    }

    @MutationMapping
    List<Tag> updateEmbeddedMcpServerTags(@Argument long id, @Argument List<TagInput> tags) {
        List<Tag> tagList = tags.stream()
            .map(tagInput -> {
                Tag tag = new Tag();

                tag.setId(tagInput.id());
                tag.setName(tagInput.name());

                return tag;
            })
            .toList();

        return embeddedMcpServerFacade.updateEmbeddedMcpServerTags(id, tagList);
    }

    @MutationMapping
    McpServer updateEmbeddedMcpServerUrl(@Argument long id) {
        return embeddedMcpServerFacade.updateEmbeddedMcpServerSecretKey(id);
    }

    @MutationMapping
    McpTool updateEmbeddedMcpTool(@Argument long id, @Argument McpToolInput input) {
        Map<String, Object> parameters = input.parameters() == null ? Map.of() : input.parameters();

        McpTool mcpTool = new McpTool(input.name(), parameters, input.mcpComponentId());

        mcpTool.setId(id);

        if (input.version() != null) {
            mcpTool.setVersion(input.version());
        }

        return embeddedMcpServerFacade.updateEmbeddedMcpTool(mcpTool);
    }

    @MutationMapping
    McpTool updateEmbeddedMcpToolEnabled(@Argument long id, @Argument boolean enabled) {
        return embeddedMcpServerFacade.updateEmbeddedMcpToolEnabled(id, enabled);
    }

    private static List<McpTool> toMcpTools(List<McpToolInputForComponent> toolInputs) {
        return toolInputs.stream()
            .map(toolInput -> new McpTool(
                toolInput.name(), toolInput.parameters() == null ? Map.of() : toolInput.parameters()))
            .toList();
    }

    record CreateEmbeddedMcpServerInput(String name, long environmentId, Boolean enabled) {
    }

    @SuppressFBWarnings("EI")
    record McpComponentWithToolsInput(
        String componentName, int componentVersion, Long mcpServerId, Long connectionId,
        List<String> requiredAuthorities, List<McpToolInputForComponent> tools, Integer version) {
    }

    record McpServerUpdateInput(
        String name, Boolean enabled, Boolean enforceToolAuthorization, Boolean authenticationRequired) {
    }

    @SuppressFBWarnings("EI")
    record McpToolInput(Long mcpComponentId, String name, Map<String, Object> parameters, Integer version) {
    }

    @SuppressFBWarnings("EI")
    record McpToolInputForComponent(String name, Map<String, Object> parameters) {
    }

    record TagInput(Long id, String name) {
    }
}
