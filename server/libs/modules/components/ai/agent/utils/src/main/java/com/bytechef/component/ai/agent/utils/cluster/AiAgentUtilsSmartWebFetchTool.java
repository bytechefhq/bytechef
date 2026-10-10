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

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;

import com.bytechef.component.ai.agent.utils.cluster.network.PrivateAddressGuardedToolCallback;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springaicommunity.agent.tools.SmartWebFetchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
public class AiAgentUtilsSmartWebFetchTool {

    public static final String DOMAIN_SAFETY_CHECK = "domainSafetyCheck";
    public static final String MAX_CONTENT_LENGTH = "maxContentLength";

    private static final String URL_ARGUMENT_NAME = "url";

    private final Set<String> allowedPrivateHosts;
    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public final ClusterElementDefinition<MultipleConnectionsToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsSmartWebFetchTool(
        ClusterElementDefinitionService clusterElementDefinitionService, Set<String> allowedPrivateHosts) {

        this.allowedPrivateHosts = Set.copyOf(allowedPrivateHosts);
        this.clusterElementDefinitionService = clusterElementDefinitionService;

        this.clusterElementDefinition =
            ComponentDsl.<MultipleConnectionsToolCallbackProviderFunction>clusterElement("smartWebFetchTool")
                .title("Smart Web Fetch Tool")
                .description("AI-powered web content summarization with caching.")
                .type(TOOLS)
                .properties(
                    bool(DOMAIN_SAFETY_CHECK)
                        .label("Domain Safety Check")
                        .description(
                            "Check each domain against an external domain safety service before fetching it. The " +
                                "domain name is sent to that service.")
                        .defaultValue(true)
                        .advancedOption(true),
                    integer(MAX_CONTENT_LENGTH)
                        .label("Max Content Length")
                        .description(
                            "Maximum number of characters of fetched content passed to the model. Leave empty for " +
                                "the default of 100000.")
                        .minValue(1)
                        .advancedOption(true))
                .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, Context context) throws Exception {

        ChatModel chatModel = resolveChatModel(extensions, componentConnections);

        ChatClient chatClient = ChatClient.builder(chatModel)
            .build();

        SmartWebFetchTool.Builder builder = SmartWebFetchTool.builder(chatClient)
            .domainSafetyCheck(inputParameters.getBoolean(DOMAIN_SAFETY_CHECK, true));

        Integer maxContentLength = inputParameters.getInteger(MAX_CONTENT_LENGTH);

        if (maxContentLength != null) {
            builder.maxContentLength(maxContentLength);
        }

        List<ToolCallback> toolCallbacks = Arrays.stream(ToolCallbacks.from(builder.build()))
            .<ToolCallback>map(toolCallback -> new PrivateAddressGuardedToolCallback(
                toolCallback, URL_ARGUMENT_NAME, allowedPrivateHosts))
            .toList();

        return ToolCallbackProvider.from(toolCallbacks);
    }

    private ChatModel resolveChatModel(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        Optional<ClusterElement> modelElement = clusterElementMap.fetchClusterElement(MODEL);

        if (modelElement.isEmpty()) {
            throw new IllegalStateException(
                "Smart Web Fetch Tool requires a Model child cluster element. Attach a chat-capable model.");
        }

        ClusterElement element = modelElement.get();

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            element.getComponentName(), element.getComponentVersion(), element.getClusterElementName());

        ComponentConnection connection = componentConnections.get(element.getWorkflowNodeName());

        Object model = modelFunction.apply(
            ParametersFactory.create(element.getParameters()),
            ParametersFactory.create(connection == null ? Map.of() : connection.getParameters()),
            false);

        if (!(model instanceof ChatModel chatModel)) {
            String returnedType = model == null ? "null" : model.getClass()
                .getName();

            throw new IllegalArgumentException(
                "MODEL child '%s' on component '%s' v%s returned %s; Smart Web Fetch Tool requires a ChatModel. Attach a chat-capable model."
                    .formatted(
                        element.getClusterElementName(), element.getComponentName(),
                        element.getComponentVersion(), returnedType));
        }

        return chatModel;
    }
}
