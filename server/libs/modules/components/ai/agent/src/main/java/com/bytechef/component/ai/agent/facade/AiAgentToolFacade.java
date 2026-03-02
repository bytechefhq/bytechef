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

package com.bytechef.component.ai.agent.facade;

import com.bytechef.ai.tool.FromAiResult;
import com.bytechef.ai.tool.facade.AbstractToolFacade;
import com.bytechef.ai.tool.util.FromAiInputSchemaUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class AiAgentToolFacade extends AbstractToolFacade {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    @SuppressFBWarnings("EI")
    public AiAgentToolFacade(
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator) {

        super(evaluator);

        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public ToolCallback getFunctionToolCallback(
        ClusterElement clusterElement, @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        ClusterElementDefinition clusterElementDefinition =
            clusterElementDefinitionService.getClusterElementDefinition(
                clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                clusterElement.getClusterElementName());

        Map<String, ?> toolParameters = clusterElement.getParameters();

        List<FromAiResult> fromAiResults = extractFromAiResults(toolParameters);

        FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback.builder(
            getToolName(clusterElementDefinition.getComponentName(), clusterElementDefinition.getName()),
            getFromAiToolCallbackFunction(
                clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                clusterElementDefinition.getName(), toolParameters, componentConnection, editorEnvironment))
            .inputType(Map.class)
            .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

        String toolDescription = getToolDescription(toolParameters, clusterElement.getExtensions());

        if (toolDescription == null) {
            toolDescription = clusterElementDefinition.getDescription();
        }

        if (toolDescription != null) {
            builder.description(toolDescription);
        }

        return builder.build();
    }

    private Function<Map<String, Object>, Object> getFromAiToolCallbackFunction(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> parameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        return request -> {
            Map<String, Object> resolvedParameters = new HashMap<>();

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                resolvedParameters.put(entry.getKey(), resolveParameterValue(entry.getValue(), request));
            }

            return clusterElementDefinitionService.executeTool(
                componentName, componentVersion, clusterElementName, MapUtils.concat(request, resolvedParameters),
                componentConnection, editorEnvironment);
        };
    }

}
