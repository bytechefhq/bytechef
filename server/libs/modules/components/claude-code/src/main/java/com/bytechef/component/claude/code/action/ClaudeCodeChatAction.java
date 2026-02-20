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

package com.bytechef.component.claude.code.action;

import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.CHAT;
import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.CHAT_PROPERTIES;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeToolFunction.CLAUDE_CODE_TOOLS;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeToolFunction;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.workflow.worker.ai.FromAiResult;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @author Ivica Cardic
 */
public class ClaudeCodeChatAction {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public static ChatActionDefinitionWrapper of(ClusterElementDefinitionService clusterElementDefinitionService) {
        return new ClaudeCodeChatAction(clusterElementDefinitionService).build();
    }

    private ClaudeCodeChatAction(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    private ChatActionDefinitionWrapper build() {
        return new ChatActionDefinitionWrapper(
            action(CHAT)
                .title("Chat")
                .description("Chat with the Claude Code agent using built-in tools.")
                .properties(CHAT_PROPERTIES)
                .output(
                    (MultipleConnectionsOutputFunction) (
                        inputParameters, componentConnections, extensions, context) -> ModelUtils.output(
                            inputParameters, null, context)));
    }

    public class ChatActionDefinitionWrapper extends AbstractActionDefinitionWrapper {

        public ChatActionDefinitionWrapper(ActionDefinition actionDefinition) {
            super(actionDefinition);
        }

        @Override
        public Optional<? extends BasePerformFunction> getPerform() {
            return Optional.of((MultipleConnectionsPerformFunction) ClaudeCodeChatAction.this::perform);
        }
    }

    private Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        ActionContext context) throws Exception {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ChatModel chatModel = resolveChatModel(inputParameters, clusterElementMap, connectionParameters);

        Path workingDirectory = Files.createTempDirectory("claude-code-");

        try {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            toolCallbacks.addAll(
                getClaudeCodeToolCallbacks(clusterElementMap, connectionParameters, workingDirectory, chatModel));

            toolCallbacks.addAll(
                getExternalToolCallbacks(clusterElementMap, connectionParameters, context.isEditorEnvironment()));

            ChatClient chatClient = ChatClient.builder(chatModel)
                .build();

            ChatClient.CallResponseSpec call = chatClient.prompt()
                .messages(ModelUtils.getMessages(inputParameters, context))
                .toolCallbacks(toolCallbacks)
                .call();

            return ModelUtils.getChatResponse(call, inputParameters, context);
        } finally {
            deleteDirectory(workingDirectory);
        }
    }

    private ChatModel resolveChatModel(
        Parameters inputParameters, ClusterElementMap clusterElementMap,
        Map<String, ComponentConnection> connectionParameters) throws Exception {

        ClusterElement modelClusterElement = clusterElementMap.getClusterElement(MODEL);

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            modelClusterElement.getComponentName(), modelClusterElement.getComponentVersion(),
            modelClusterElement.getClusterElementName());

        ComponentConnection modelConnection = connectionParameters.get(modelClusterElement.getWorkflowNodeName());

        if (modelConnection == null) {
            throw new IllegalStateException(
                "No connection found for model cluster element: " + modelClusterElement.getWorkflowNodeName());
        }

        return (ChatModel) modelFunction.apply(
            ParametersFactory.create(
                MapUtils.concat(
                    new HashMap<>(inputParameters.toMap()), new HashMap<>(modelClusterElement.getParameters()))),
            ParametersFactory.create(modelConnection.getParameters()), true);
    }

    private List<ToolCallback> getClaudeCodeToolCallbacks(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> connectionParameters,
        Path workingDirectory, ChatModel chatModel) throws Exception {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement clusterElement : clusterElementMap.getClusterElements(CLAUDE_CODE_TOOLS)) {
            ClaudeCodeToolFunction toolFunction = clusterElementDefinitionService.getClusterElement(
                clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                clusterElement.getClusterElementName());

            ComponentConnection componentConnection = connectionParameters.get(clusterElement.getWorkflowNodeName());

            Parameters toolConnectionParameters = ParametersFactory.create(
                componentConnection == null ? Map.of() : componentConnection.getParameters());

            toolCallbacks.addAll(
                toolFunction.apply(
                    ParametersFactory.create(clusterElement.getParameters()), toolConnectionParameters,
                    workingDirectory, chatModel));
        }

        return toolCallbacks;
    }

    private List<ToolCallback> getExternalToolCallbacks(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> connectionParameters,
        boolean editorEnvironment) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement clusterElement : clusterElementMap.getClusterElements(BaseToolFunction.TOOLS)) {
            ClusterElementDefinition clusterElementDefinition =
                clusterElementDefinitionService.getClusterElementDefinition(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElement.getClusterElementName());

            ComponentConnection componentConnection = connectionParameters.get(clusterElement.getWorkflowNodeName());

            Map<String, ?> toolParameters = clusterElement.getParameters();

            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback.builder(
                clusterElementDefinition.getName(),
                getToolCallbackFunction(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElementDefinition.getName(), toolParameters, componentConnection, editorEnvironment))
                .inputType(Map.class)
                .inputSchema(
                    JsonSchemaGeneratorUtils.generateInputSchema(clusterElementDefinition.getProperties()));

            String description = clusterElementDefinition.getDescription();

            if (description != null) {
                builder.description(description);
            }

            toolCallbacks.add(builder.build());
        }

        return toolCallbacks;
    }

    private java.util.function.Function<Map<String, Object>, Object> getToolCallbackFunction(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> parameters,
        ComponentConnection componentConnection, boolean editorEnvironment) {

        return request -> {
            Map<String, Object> resolvedParameters = new HashMap<>();

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof FromAiResult fromAiResult) {
                    Object requestValue = request.get(fromAiResult.name());

                    resolvedParameters.put(
                        entry.getKey(), requestValue != null ? requestValue : fromAiResult.defaultValue());
                } else {
                    resolvedParameters.put(entry.getKey(), value);
                }
            }

            return clusterElementDefinitionService.executeTool(
                componentName, componentVersion, clusterElementName, MapUtils.concat(request, resolvedParameters),
                componentConnection, editorEnvironment);
        };
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                Files.delete(directory);

                return FileVisitResult.CONTINUE;
            }
        });
    }
}
