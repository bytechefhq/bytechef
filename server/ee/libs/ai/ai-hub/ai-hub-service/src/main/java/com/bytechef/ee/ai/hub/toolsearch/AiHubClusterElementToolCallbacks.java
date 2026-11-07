/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.commons.util.MemoizationUtils;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalDefaults;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.ee.ai.hub.util.ToolNameNormalizer;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubClusterElementToolCallbacks {

    private static final Logger log = LoggerFactory.getLogger(AiHubClusterElementToolCallbacks.class);

    private final Supplier<Map<String, ClusterElementToolCallback>> allSupplier;
    private final Supplier<Map<String, ClusterElementToolCallback>> readOnlySupplier;

    public AiHubClusterElementToolCallbacks(Supplier<Map<String, ClusterElementToolCallback>> callbacksSupplier) {
        this.allSupplier = MemoizationUtils.memoize(() -> Map.copyOf(callbacksSupplier.get()));
        this.readOnlySupplier = MemoizationUtils.memoize(() -> readOnly(allSupplier.get()));
    }

    public static AiHubClusterElementToolCallbacks of(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ConnectionDefinitionService connectionDefinitionService, CopilotConnectionLister connectionLister) {

        return new AiHubClusterElementToolCallbacks(
            () -> build(
                clusterElementDefinitionService, clusterElementDefinitionFacade, connectionDefinitionService,
                connectionLister));
    }

    public static boolean isReadOnly(String clusterElementName) {
        return !AiHubToolApprovalDefaults.isComponentToolGatedByDefault(clusterElementName);
    }

    public Map<String, ClusterElementToolCallback> toolCallbacks(Mode mode) {
        return mode == Mode.BUILD ? allSupplier.get() : readOnlySupplier.get();
    }

    public Optional<ClusterElementToolCallback> findToolCallback(Mode mode, String toolName) {
        Map<String, ClusterElementToolCallback> toolCallbacks = toolCallbacks(mode);

        return Optional.ofNullable(toolCallbacks.get(toolName));
    }

    private static Map<String, ClusterElementToolCallback> build(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ConnectionDefinitionService connectionDefinitionService, CopilotConnectionLister connectionLister) {

        List<ClusterElementDefinition> clusterElementDefinitions =
            clusterElementDefinitionService.getClusterElementDefinitionStubs(BaseToolFunction.TOOLS);

        Map<String, ClusterElementToolCallback> toolCallbacks = new LinkedHashMap<>();

        for (ClusterElementDefinition clusterElementDefinition : clusterElementDefinitions) {
            String summary = ToolSearchCatalogFeeder.buildSummary(clusterElementDefinition);

            if (summary == null) {
                continue;
            }

            String toolName = ToolNameNormalizer.toToolName(
                clusterElementDefinition.getComponentName(), clusterElementDefinition.getName());

            ClusterElementToolCallback toolCallback = new ClusterElementToolCallback(
                toolName, summary, clusterElementDefinition.getComponentName(),
                clusterElementDefinition.getComponentVersion(), clusterElementDefinition.getName(),
                clusterElementDefinitionService, clusterElementDefinitionFacade, connectionDefinitionService,
                connectionLister);

            ClusterElementToolCallback existingToolCallback = toolCallbacks.get(toolName);

            if (existingToolCallback == null) {
                toolCallbacks.put(toolName, toolCallback);
            } else if (!existingToolCallback.getComponentName()
                .equals(toolCallback.getComponentName())) {

                log.warn(
                    "Cluster element tool name {} is taken by component {}, so {} {} is left out", toolName,
                    existingToolCallback.getComponentName(), toolCallback.getComponentName(),
                    toolCallback.getClusterElementName());
            } else if (toolCallback.getComponentVersion() > existingToolCallback.getComponentVersion()) {
                toolCallbacks.put(toolName, toolCallback);
            }
        }

        return toolCallbacks;
    }

    private static Map<String, ClusterElementToolCallback> readOnly(
        Map<String, ClusterElementToolCallback> toolCallbacks) {

        Map<String, ClusterElementToolCallback> readOnlyToolCallbacks = new LinkedHashMap<>();

        for (Map.Entry<String, ClusterElementToolCallback> entry : toolCallbacks.entrySet()) {
            ClusterElementToolCallback toolCallback = entry.getValue();

            if (isReadOnly(toolCallback.getClusterElementName())) {
                readOnlyToolCallbacks.put(entry.getKey(), toolCallback);
            }
        }

        return Map.copyOf(readOnlyToolCallbacks);
    }
}
