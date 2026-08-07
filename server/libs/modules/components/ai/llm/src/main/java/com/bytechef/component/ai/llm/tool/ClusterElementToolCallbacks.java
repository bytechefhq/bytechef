/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.llm.tool;

import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the {@link ToolCallback}s a TOOLS cluster element contributes. A TOOLS element comes in four shapes and every
 * caller must handle all four; keeping the dispatch here means "what shapes are there?" has one answer instead of one
 * per call site.
 *
 * <p>
 * It previously had two, and they had drifted: the copy in the task tool's subagent path omitted the
 * {@link ToolCallbackProviderFunction} branch, so ten of the thirteen aiAgentUtils tool elements fell through to the
 * facade and produced a tool built from the element's parameters rather than from the provider.
 *
 * @author Ivica Cardic
 */
public class ClusterElementToolCallbacks {

    private final AiAgentToolFacade aiAgentToolFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;

    @SuppressFBWarnings("EI")
    public ClusterElementToolCallbacks(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService) {

        this.aiAgentToolFacade = aiAgentToolFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public List<ToolCallback> build(
        ClusterElement clusterElement, Map<String, ComponentConnection> componentConnections,
        boolean editorEnvironment, ActionContext context) {

        Object clusterElementFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        if (clusterElementFunction instanceof MultipleConnectionsToolCallbackProviderFunction providerFunction) {
            try {
                ToolCallback[] providerToolCallbacks = providerFunction
                    .apply(
                        ParametersFactory.create(clusterElement.getParameters()),
                        getConnectionParameters(componentConnections, clusterElement),
                        ParametersFactory.create(clusterElement.getExtensions()),
                        componentConnections, context)
                    .getToolCallbacks();

                return Arrays.asList(providerToolCallbacks);
            } catch (Exception exception) {
                throw initializationException(clusterElement, exception, context);
            }
        } else if (clusterElementFunction instanceof ToolCallbackProviderFunction toolCallbackProviderFunction) {
            try {
                ComponentConnection componentConnection = componentConnections.get(
                    clusterElement.getWorkflowNodeName());

                ToolCallback[] providerToolCallbacks = toolCallbackProviderFunction
                    .apply(
                        ParametersFactory.create(clusterElement.getParameters()),
                        ParametersFactory.create(componentConnection), context)
                    .getToolCallbacks();

                return Arrays.asList(providerToolCallbacks);
            } catch (Exception exception) {
                throw initializationException(clusterElement, exception, context);
            }
        } else if (clusterElementFunction instanceof MultipleConnectionsToolFunction) {
            return List.of(
                aiAgentToolFacade.getFunctionToolCallback(clusterElement, componentConnections, editorEnvironment));
        } else {
            ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

            return List.of(
                aiAgentToolFacade.getFunctionToolCallback(clusterElement, componentConnection, editorEnvironment));
        }
    }

    private static Parameters getConnectionParameters(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement) {

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        return ParametersFactory.create(componentConnection);
    }

    private static RuntimeException initializationException(
        ClusterElement clusterElement, Throwable cause, ActionContext context) {

        Class<? extends Throwable> causeClass = cause.getClass();

        String message = String.format(
            "Failed to initialize tool callback for cluster element '%s' (component=%s v%d): %s",
            clusterElement.getClusterElementName(), clusterElement.getComponentName(),
            clusterElement.getComponentVersion(),
            cause.getMessage() == null ? causeClass.getSimpleName() : cause.getMessage());

        context.log(log -> log.error(message, cause));

        return new IllegalStateException(message, cause);
    }
}
