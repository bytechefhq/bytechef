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

package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.component.definition.ai.agent.SubagentFunction.SUBAGENT;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;

import com.bytechef.component.ai.agent.utils.cluster.subagent.ClusterElementSubagentExecutor;
import com.bytechef.component.ai.agent.utils.cluster.subagent.ClusterElementSubagentResolver;
import com.bytechef.component.ai.agent.utils.cluster.subagent.TenantAwareTaskRepository;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.agent.common.task.subagent.SubagentType;
import org.springaicommunity.agent.tools.task.TaskOutputTool;
import org.springaicommunity.agent.tools.task.TaskTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Provides a task tool that delegates complex tasks to specialized sub-agents.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsTaskTool {

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ClusterElementToolCallbacks clusterElementToolCallbacks;

    public final ClusterElementDefinition<MultipleConnectionsToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsTaskTool(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.clusterElementToolCallbacks =
            new ClusterElementToolCallbacks(aiAgentToolFacade, clusterElementDefinitionService);

        this.clusterElementDefinition =
            ComponentDsl.<MultipleConnectionsToolCallbackProviderFunction>clusterElement("taskTool")
                .title("Task Tool")
                .description("Delegate tasks to subagents you define, each limited to the tools you attach to it.")
                .type(TOOLS)
                .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, Context context) throws Exception {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ChatModel defaultChatModel = resolveChatModel(clusterElementMap, componentConnections);

        List<ClusterElement> subagentClusterElements = clusterElementMap.getClusterElements(SUBAGENT);

        ClusterElementSubagentResolver subagentResolver = new ClusterElementSubagentResolver(subagentClusterElements);

        ClusterElementSubagentExecutor subagentExecutor = new ClusterElementSubagentExecutor(
            defaultChatModel,
            subagentClusterElement -> resolveChatModel(
                ClusterElementMap.of(subagentClusterElement.getExtensions()), componentConnections),
            subagentClusterElement -> buildSubagentToolCallbacks(
                subagentClusterElement, componentConnections, context));

        TenantAwareTaskRepository taskRepository = new TenantAwareTaskRepository(executorService);

        ToolCallback taskToolCallback = TaskTool.builder()
            .subagentTypes(new SubagentType(subagentResolver, subagentExecutor))
            .subagentReferences(subagentResolver.getReferences())
            .taskRepository(taskRepository)
            .build();

        ToolCallback taskOutputToolCallback = TaskOutputTool.builder()
            .taskRepository(taskRepository)
            .build();

        return ToolCallbackProvider.from(taskToolCallback, taskOutputToolCallback);
    }

    private List<ToolCallback> buildSubagentToolCallbacks(
        ClusterElement subagentClusterElement, Map<String, ComponentConnection> componentConnections,
        Context context) {

        ClusterElementMap subagentClusterElementMap = ClusterElementMap.of(subagentClusterElement.getExtensions());

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement toolClusterElement : subagentClusterElementMap.getClusterElements(TOOLS)) {
            toolCallbacks.addAll(
                clusterElementToolCallbacks.build(
                    toolClusterElement, componentConnections, (ActionContext) context));
        }

        return toolCallbacks;
    }

    @Nullable
    private ChatModel resolveChatModel(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> componentConnections) {

        Optional<ClusterElement> modelElement = clusterElementMap.fetchClusterElement(MODEL);

        if (modelElement.isEmpty()) {
            return null;
        }

        ClusterElement element = modelElement.get();

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            element.getComponentName(), element.getComponentVersion(), element.getClusterElementName());

        ComponentConnection connection = componentConnections.get(element.getWorkflowNodeName());

        Object model;

        try {
            model = modelFunction.apply(
                ParametersFactory.create(element.getParameters()),
                ParametersFactory.create(connection == null ? Map.of() : connection.getParameters()),
                false);
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Unable to initialize the model '%s'".formatted(element.getWorkflowNodeName()), exception);
        }

        if (!(model instanceof ChatModel chatModel)) {
            String returnedType = model == null ? "null" : model.getClass()
                .getName();

            throw new IllegalArgumentException(
                "MODEL child '%s' on component '%s' v%s returned %s; Task Tool requires a ChatModel. Attach a chat-capable model."
                    .formatted(
                        element.getClusterElementName(), element.getComponentName(),
                        element.getComponentVersion(), returnedType));
        }

        return chatModel;
    }
}
