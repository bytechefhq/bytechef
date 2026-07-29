/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * Resolves the per-node connection wiring for a connected user's reference to a shared catalog workflow. A small,
 * deliberately separate class rather than a refactor of {@link ConnectedUserProjectWorkflowManager}'s private
 * {@code checkWorkflowNodeConnection(s)} methods -- those write into {@code WorkflowTestConfiguration}, which is wrong
 * for a shared catalog workflow (two connected users referencing the same workflow must never see each other's
 * connections). Duplicating the small node-scanning loop here is safer than risking the existing, proven copy-mode
 * path.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectedUserWorkflowConnectionResolver {

    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public ConnectedUserWorkflowConnectionResolver(
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.connectionService = connectionService;
    }

    /**
     * @throws MissingConnectionException if a node's component declares a connection definition and the connected user
     *                                    has no matching connection to auto-wire.
     */
    public Map<String, Long> resolve(String definition) {
        Map<String, ?> workflowMap = JsonUtils.readMap(definition);
        List<Connection> connections = connectionService.getConnections(PlatformType.EMBEDDED);

        Map<String, Long> resolved = new LinkedHashMap<>();

        for (Map<String, ?> nodeMap : allNodes(workflowMap)) {
            String nodeName = MapUtils.getString(nodeMap, "name");
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(MapUtils.getString(nodeMap, "type"));

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                workflowNodeType.name(), workflowNodeType.version());

            // A component definition that could not be resolved is treated conservatively -- as possibly requiring
            // a connection -- rather than silently skipped, so a node whose metadata is unavailable never ends up
            // wired to nothing.
            if (componentDefinition != null && componentDefinition.getConnection() == null) {
                continue;
            }

            Connection connection = connections.stream()
                .filter(candidate -> Objects.equals(candidate.getComponentName(), workflowNodeType.name()))
                .findFirst()
                .orElseThrow(() -> new MissingConnectionException(workflowNodeType.name()));

            resolved.put(nodeName, connection.getId());
        }

        return resolved;
    }

    private static List<Map<String, ?>> allNodes(Map<String, ?> workflowMap) {
        List<Map<String, ?>> nodes = new ArrayList<>();

        nodes.addAll(MapUtils.getList(workflowMap, "triggers", new TypeReference<>() {}, List.of()));
        nodes.addAll(MapUtils.getList(workflowMap, "tasks", new TypeReference<>() {}, List.of()));

        return nodes;
    }
}
