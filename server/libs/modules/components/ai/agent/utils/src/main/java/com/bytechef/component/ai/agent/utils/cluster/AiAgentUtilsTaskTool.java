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

import com.bytechef.component.ai.agent.utils.cluster.subagent.ByteChefSubagentExecutor;
import com.bytechef.component.ai.agent.utils.cluster.subagent.ByteChefSubagentResolver;
import com.bytechef.component.ai.agent.utils.cluster.subagent.ByteChefTaskRepository;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
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
 * Provides a task tool that delegates work to subagents the workflow builder defines as SUBAGENT child cluster
 * elements. Each subagent's persona, tools and model come from its own element, so it can call nothing that was not
 * attached to it, and one subagent can run on a different model than its siblings. A Model attached to this tool is the
 * fallback for subagents that declare none.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsTaskTool {

    /**
     * Shared across runs because a repository is built per invocation while the executor must outlive any one of them.
     * Virtual threads keep an idle pool free, and the component handler is a singleton, so this lives for the
     * application's lifetime and is never shut down by a repository.
     */
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

        // Optional: covers only the subagents that declare no Model of their own.
        ChatModel defaultChatModel = resolveChatModel(clusterElementMap, componentConnections);

        List<ClusterElement> subagentClusterElements = clusterElementMap.getClusterElements(SUBAGENT);

        ByteChefSubagentResolver subagentResolver = new ByteChefSubagentResolver(subagentClusterElements);

        boolean editorEnvironment = ((ActionContextAware) context).isEditorEnvironment();

        ByteChefSubagentExecutor subagentExecutor = new ByteChefSubagentExecutor(
            defaultChatModel,
            // Built from the subagent's OWN extensions, so one subagent can never be run on a sibling's model.
            subagentClusterElement -> resolveChatModel(
                ClusterElementMap.of(subagentClusterElement.getExtensions()), componentConnections),
            subagentClusterElement -> buildSubagentToolCallbacks(
                subagentClusterElement, componentConnections, editorEnvironment, context));

        // One repository per invocation, so background task ids from one run can never be read by another.
        ByteChefTaskRepository taskRepository = new ByteChefTaskRepository(executorService);

        ToolCallback taskToolCallback = TaskTool.builder()
            .subagentTypes(new SubagentType(subagentResolver, subagentExecutor))
            .subagentReferences(subagentResolver.getReferences())
            .taskRepository(taskRepository)
            .build();

        // Without this the model can start a background task but never read its result.
        ToolCallback taskOutputToolCallback = TaskOutputTool.builder()
            .taskRepository(taskRepository)
            .build();

        return ToolCallbackProvider.from(taskToolCallback, taskOutputToolCallback);
    }

    /**
     * Turns one subagent's nested TOOLS children into callbacks.
     *
     * <p>
     * The cluster element map is built from <b>this subagent's own</b> extensions. That is what makes a subagent
     * structurally unable to reach a sibling's tools — it never sees them. Hoisting this map up to the task tool would
     * silently hand every subagent every tool.
     */
    private List<ToolCallback> buildSubagentToolCallbacks(
        ClusterElement subagentClusterElement, Map<String, ComponentConnection> componentConnections,
        boolean editorEnvironment, Context context) {

        ClusterElementMap subagentClusterElementMap = ClusterElementMap.of(subagentClusterElement.getExtensions());

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement toolClusterElement : subagentClusterElementMap.getClusterElements(TOOLS)) {
            // A gate here would suspend on the AGENT's context and return the sentinel into the subagent's own
            // ChatClient, which treats it as an ordinary result and continues; the parent then cannot find the
            // sentinel among its own tool responses and the suspend is orphaned.
            if (AiAgentUtilsApprovalGateTool.APPROVAL_GATE.equals(toolClusterElement.getClusterElementName())) {
                throw new IllegalStateException(
                    "Approval gates cannot be attached to a subagent — a suspended subagent run cannot be resumed. " +
                        "Attach the gate to the agent instead.");
            }

            toolCallbacks.addAll(
                clusterElementToolCallbacks.build(
                    toolClusterElement, componentConnections, editorEnvironment, (ActionContext) context));
        }

        return toolCallbacks;
    }

    /**
     * Resolves the MODEL child of one cluster element map, or null when none is attached. Null is a valid answer at
     * both call sites: a subagent without a Model falls back to the Task Tool's, and a Task Tool without one is fine as
     * long as every subagent brings its own. Only a subagent left with neither fails, and it fails in the executor
     * where the subagent's name can be named.
     */
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

        // Resolution is lazy — a subagent's model is built when that subagent first runs — so the checked exception
        // cannot propagate out of the java.util.function.Function seam the executor calls this through.
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
