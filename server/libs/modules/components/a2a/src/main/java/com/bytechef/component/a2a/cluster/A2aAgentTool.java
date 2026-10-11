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

package com.bytechef.component.a2a.cluster;

import static com.bytechef.component.a2a.constant.A2aConstants.AGENT_URL;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.tool.constant.ToolConstants;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @author Ivica Cardic
 */
public class A2aAgentTool {

    private static final String AGENT_CARD_PATH = ".well-known/agent-card.json";

    private static final long RESPONSE_TIMEOUT_SECONDS = 60;
    private static final String DEFAULT_TOOL_DESCRIPTION =
        "Delegate a task to the configured remote A2A agent and return its response. Provide the task as a " +
            "self-contained instruction.";
    private static final String DEFAULT_TOOL_NAME = "sendTaskToRemoteAgent";

    public static final ClusterElementDefinition<ToolCallbackProviderFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ToolCallbackProviderFunction>clusterElement("agentTool")
            .title("A2A Agent Tool")
            .description("Delegate a task to a remote A2A (Agent2Agent) agent.")
            .type(TOOLS)
            .properties(
                string(AGENT_URL)
                    .label("Agent URL")
                    .description(
                        "The URL of the remote A2A agent - for a ByteChef A2A Server, the endpoint URL from the " +
                            "server's Connect tab. The agent card is read from " +
                            "<Agent URL>/.well-known/agent-card.json.")
                    .required(true))
            .object(() -> A2aAgentTool::apply);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    static ToolCallbackProvider apply(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String agentUrl = inputParameters.getRequiredString(AGENT_URL);
        Map<String, String> headers = getAuthorizationHeaders(connectionParameters);

        Function<RemoteAgentTask, String> sendTaskFunction =
            remoteAgentTask -> sendTask(agentUrl, headers, remoteAgentTask.task());

        ToolCallback toolCallback = FunctionToolCallback.builder(
            getNonBlankString(inputParameters, ToolConstants.TOOL_NAME, DEFAULT_TOOL_NAME), sendTaskFunction)
            .inputType(RemoteAgentTask.class)
            .description(getNonBlankString(inputParameters, ToolConstants.TOOL_DESCRIPTION, DEFAULT_TOOL_DESCRIPTION))
            .build();

        return ToolCallbackProvider.from(toolCallback);
    }

    private static String sendTask(String baseUrl, Map<String, String> headers, String task) {
        StringBuilder responseBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorReference = new AtomicReference<>();

        try {
            AgentCard agentCard = withEndpointUrl(
                A2A.getAgentCard(getAgentCardBaseUrl(baseUrl), AGENT_CARD_PATH, headers), getEndpointUrl(baseUrl));

            Client client = Client.builder(agentCard)
                .clientConfig(new ClientConfig.Builder()
                    .setStreaming(false)
                    .setAcceptedOutputModes(List.of("text"))
                    .build())
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                .addConsumer((clientEvent, card) -> {
                    appendEventText(responseBuilder, clientEvent);

                    latch.countDown();
                })
                .streamingErrorHandler(throwable -> {
                    errorReference.set(throwable);

                    latch.countDown();
                })
                .build();

            try {
                client.sendMessage(A2A.toUserMessage(task), new ClientCallContext(Map.of(), headers));

                boolean completed = latch.await(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (!completed) {
                    throw new IllegalStateException(
                        "Timed out waiting for a response from the remote agent at " + baseUrl);
                }

                Throwable error = errorReference.get();

                if (error != null) {
                    throw new IllegalStateException(
                        "Remote agent at " + baseUrl + " returned an error: " + error.getMessage(), error);
                }

                return responseBuilder.toString();
            } finally {
                client.close();
            }
        } catch (IllegalStateException illegalStateException) {
            throw illegalStateException;
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Failed to send task to the remote agent at " + baseUrl + ": " + exception.getMessage(), exception);
        }
    }

    static Map<String, String> getAuthorizationHeaders(Parameters connectionParameters) {
        String token = connectionParameters.getString(TOKEN);

        if (token == null || token.isBlank()) {
            return Map.of();
        }

        return Map.of("Authorization", "Bearer " + token);
    }

    static String getAgentCardBaseUrl(String baseUrl) {
        String agentCardBaseUrl = baseUrl.strip();

        if (agentCardBaseUrl.endsWith(AGENT_CARD_PATH)) {
            agentCardBaseUrl = agentCardBaseUrl.substring(0, agentCardBaseUrl.length() - AGENT_CARD_PATH.length());
        }

        return agentCardBaseUrl.endsWith("/") ? agentCardBaseUrl : agentCardBaseUrl + "/";
    }

    static String getEndpointUrl(String baseUrl) {
        String agentCardBaseUrl = getAgentCardBaseUrl(baseUrl);

        return agentCardBaseUrl.substring(0, agentCardBaseUrl.length() - 1);
    }

    static AgentCard withEndpointUrl(AgentCard agentCard, String endpointUrl) {
        List<AgentInterface> additionalInterfaces = agentCard.additionalInterfaces();

        AgentCard.Builder agentCardBuilder = new AgentCard.Builder(agentCard)
            .url(endpointUrl);

        if (additionalInterfaces != null) {
            agentCardBuilder.additionalInterfaces(
                additionalInterfaces.stream()
                    .map(agentInterface -> new AgentInterface(agentInterface.transport(), endpointUrl))
                    .toList());
        }

        return agentCardBuilder.build();
    }

    private static String getNonBlankString(Parameters inputParameters, String key, String defaultValue) {
        String value = inputParameters.getString(key);

        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static void appendEventText(StringBuilder responseBuilder, ClientEvent clientEvent) {
        if (clientEvent instanceof MessageEvent messageEvent) {
            appendMessageText(responseBuilder, messageEvent.getMessage());
        } else if (clientEvent instanceof TaskEvent taskEvent) {
            appendTaskText(responseBuilder, taskEvent.getTask());
        } else if (clientEvent instanceof TaskUpdateEvent taskUpdateEvent) {
            appendTaskText(responseBuilder, taskUpdateEvent.getTask());
        }
    }

    private static void appendTaskText(StringBuilder responseBuilder, Task task) {
        List<Artifact> artifacts = task.getArtifacts();

        if (artifacts != null && !artifacts.isEmpty()) {
            for (Artifact artifact : artifacts) {
                appendParts(responseBuilder, artifact.parts());
            }

            return;
        }

        TaskStatus taskStatus = task.getStatus();

        if (taskStatus != null && taskStatus.message() != null) {
            appendMessageText(responseBuilder, taskStatus.message());
        }
    }

    private static void appendMessageText(StringBuilder responseBuilder, Message message) {
        if (message != null) {
            appendParts(responseBuilder, message.getParts());
        }
    }

    private static void appendParts(StringBuilder responseBuilder, List<Part<?>> parts) {
        if (parts == null) {
            return;
        }

        for (Part<?> part : parts) {
            if (part instanceof TextPart textPart) {
                responseBuilder.append(textPart.getText());
            }
        }
    }

    public record RemoteAgentTask(String task) {
    }
}
