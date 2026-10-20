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

package com.bytechef.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ConverterAgentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallReturnsResultWhenSubagentSucceeds() {
        String synthesised = "{\"label\":\"converted from n8n\",\"tasks\":[]}";

        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(synthesised);

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"request\":\"convert this n8n workflow\"}");

        assertThat(result).isEqualTo(synthesised);
    }

    @Test
    void testCallWithAProjectIdCreatesTheWorkflowThenConvertsIntoIt() {
        ChatClient chatClient = stubbedChatClient("converted");
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = newTargetWorkflowManager();

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(
            chatModel -> chatClient, null, targetWorkflowManager);

        String result = callback.call("{\"request\":\"convert this n8n workflow\",\"projectId\":7}");

        assertThat(result).startsWith("converted");

        ArgumentCaptor<ToolContext> toolContextCaptor = ArgumentCaptor.forClass(ToolContext.class);

        verify(targetWorkflowManager).createWorkflow(eq(7L), toolContextCaptor.capture());

        assertThat(toolContextCaptor.getValue()
            .getContext()).containsKey("bytechef.workflowEditor.persistedWorkflows");

        verify(chatClient).prompt("Target workflowId: wf-new\n\nconvert this n8n workflow");
    }

    @Test
    void testSuccessfulConversionKeepsTheCreatedWorkflow() {
        ChatClient chatClient = chatClientUpdatingWorkflow("wf-new", "converted");
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = newTargetWorkflowManager();

        String result = new ConverterAgentToolCallback(chatModel -> chatClient, null, targetWorkflowManager)
            .call("{\"request\":\"convert\",\"projectId\":7}");

        assertThat(result)
            .startsWith("converted")
            .doesNotContain("has no converted definition yet");

        verify(targetWorkflowManager, never()).deleteWorkflow(anyString(), any(ToolContext.class));
    }

    @Test
    void testFailedConversionDeletesTheCreatedWorkflow() {
        ChatClient chatClient = mock(ChatClient.class);

        when(chatClient.prompt(anyString())).thenThrow(new IllegalStateException("conversion engine failure"));

        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = newTargetWorkflowManager();

        String result = new ConverterAgentToolCallback(chatModel -> chatClient, null, targetWorkflowManager)
            .call("{\"request\":\"convert\",\"projectId\":7}");

        assertThat(result).contains("error");

        ArgumentCaptor<ToolContext> createContextCaptor = ArgumentCaptor.forClass(ToolContext.class);
        ArgumentCaptor<ToolContext> deleteContextCaptor = ArgumentCaptor.forClass(ToolContext.class);

        verify(targetWorkflowManager).createWorkflow(eq(7L), createContextCaptor.capture());
        verify(targetWorkflowManager).deleteWorkflow(eq("wf-new"), deleteContextCaptor.capture());

        assertThat(deleteContextCaptor.getValue()
            .getContext()).isEqualTo(createContextCaptor.getValue()
                .getContext());
    }

    @Test
    void testErrorPayloadDeletesTheCreatedWorkflowAndReturnsTheError() {
        ChatClient chatClient = stubbedChatClient("{\"error\":\"could not map the n8n nodes\"}");
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = newTargetWorkflowManager();

        String result = new ConverterAgentToolCallback(chatModel -> chatClient, null, targetWorkflowManager)
            .call("{\"request\":\"convert\",\"projectId\":7}");

        assertThat(result).isEqualTo("{\"error\":\"could not map the n8n nodes\"}");

        verify(targetWorkflowManager).deleteWorkflow(eq("wf-new"), any(ToolContext.class));
    }

    @Test
    void testUnavailableChatModelFailsBeforeAnyWorkflowIsCreated() {
        SubAgentChatModelResolver chatModelResolver = mock(SubAgentChatModelResolver.class);

        when(chatModelResolver.resolve(any())).thenThrow(new ChatModelUnavailableException("no model"));

        ChatClient chatClient = mock(ChatClient.class);
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = newTargetWorkflowManager();

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(
            chatModel -> chatClient, chatModelResolver, targetWorkflowManager);

        assertThatThrownBy(() -> callback.call("{\"request\":\"convert\",\"projectId\":7}"))
            .isInstanceOf(ChatModelUnavailableException.class);

        verify(targetWorkflowManager, never()).createWorkflow(anyLong(), any(ToolContext.class));
        verify(targetWorkflowManager, never()).deleteWorkflow(anyString(), any(ToolContext.class));
        verifyNoInteractions(chatClient);
    }

    @Test
    void testNullSubAgentResultDeletesTheCreatedWorkflow() {
        ChatClient chatClient = stubbedChatClient(null);
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = newTargetWorkflowManager();

        String result = new ConverterAgentToolCallback(chatModel -> chatClient, null, targetWorkflowManager)
            .call("{\"request\":\"convert\",\"projectId\":7}");

        assertThat(result).contains("returned null");

        verify(targetWorkflowManager).deleteWorkflow(eq("wf-new"), any(ToolContext.class));
    }

    @Test
    void testErrorAfterTheSubAgentWroteTheWorkflowKeepsIt() {
        ChatClient chatClient = chatClientUpdatingWorkflow("wf-new", "{\"error\":\"validation warning\"}");
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = newTargetWorkflowManager();

        new ConverterAgentToolCallback(chatModel -> chatClient, null, targetWorkflowManager)
            .call("{\"request\":\"convert\",\"projectId\":7}");

        verify(targetWorkflowManager, never()).deleteWorkflow(anyString(), any(ToolContext.class));
    }

    @Test
    void testAnswerWithoutAWrittenDefinitionKeepsTheWorkflowAndReturnsItsId() {
        ChatClient chatClient = stubbedChatClient("Which Slack channel should the converted workflow post to?");
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = newTargetWorkflowManager();

        String result = new ConverterAgentToolCallback(chatModel -> chatClient, null, targetWorkflowManager)
            .call("{\"request\":\"convert\",\"projectId\":7}");

        assertThat(result)
            .startsWith("Which Slack channel")
            .contains("workflowId=\"wf-new\"")
            .contains("Call importWorkflow again with this workflowId");

        verify(targetWorkflowManager, never()).deleteWorkflow(anyString(), any(ToolContext.class));
    }

    @Test
    void testDeleteFailureStillReturnsTheConversionError() {
        ChatClient chatClient = stubbedChatClient("{\"error\":\"could not map the n8n nodes\"}");
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = newTargetWorkflowManager();

        doThrow(new IllegalStateException("delete denied")).when(targetWorkflowManager)
            .deleteWorkflow(eq("wf-new"), any(ToolContext.class));

        String result = new ConverterAgentToolCallback(chatModel -> chatClient, null, targetWorkflowManager)
            .call("{\"request\":\"convert\",\"projectId\":7}");

        assertThat(result).contains("could not map the n8n nodes");
    }

    @Test
    void testCallWithAWorkflowIdConvertsIntoItWithoutCreatingOne() {
        ChatClient chatClient = stubbedChatClient("converted");
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = mock(
            ConverterAgentToolCallback.TargetWorkflowManager.class);

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(
            chatModel -> chatClient, null, targetWorkflowManager);

        String result = callback.call("{\"request\":\"convert this n8n workflow\",\"workflowId\":\"wf-1\"}");

        assertThat(result).isEqualTo("converted");

        verify(chatClient).prompt("Target workflowId: wf-1\n\nconvert this n8n workflow");
        verifyNoInteractions(targetWorkflowManager);
    }

    @Test
    void testCallWithBothAProjectIdAndAWorkflowIdIsAToolErrorWithoutASubAgentCall() {
        ChatClient chatClient = mock(ChatClient.class);
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = mock(
            ConverterAgentToolCallback.TargetWorkflowManager.class);

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(
            chatModel -> chatClient, null, targetWorkflowManager);

        String result = callback.call("{\"request\":\"convert\",\"projectId\":7,\"workflowId\":\"wf-1\"}");

        assertThat(result)
            .contains("error")
            .contains("not both");

        verifyNoInteractions(chatClient, targetWorkflowManager);
    }

    @Test
    void testCallWithNeitherIdPassesTheRequestThroughUnchanged() {
        ChatClient chatClient = stubbedChatClient("converted");

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"request\":\"convert into workflowId wf-1: {}\"}");

        assertThat(result).isEqualTo("converted");

        verify(chatClient).prompt("convert into workflowId wf-1: {}");
    }

    @Test
    void testCallWithAProjectIdButNoTargetWorkflowManagerIsAToolError() {
        ChatClient chatClient = mock(ChatClient.class);

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"request\":\"convert\",\"projectId\":7}");

        assertThat(result)
            .contains("error")
            .contains("create it first and pass its workflowId");

        verifyNoInteractions(chatClient);
    }

    @Test
    void testToolDefinitionDeclaresOptionalProjectIdAndWorkflowId() throws Exception {
        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> mock(ChatClient.class), null);

        JsonNode inputSchema = jsonMapper.readTree(callback.getToolDefinition()
            .inputSchema());

        assertThat(inputSchema.at("/properties/projectId/type")
            .asText()).isEqualTo("integer");
        assertThat(inputSchema.at("/properties/workflowId/type")
            .asText()).isEqualTo("string");
        assertThat(inputSchema.get("required")
            .toString()).isEqualTo("[\"request\"]");
    }

    @Test
    void testCallReturnsErrorWhenRequestIsBlank() {
        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> mock(ChatClient.class), null);

        String result = callback.call("{\"request\":\"   \"}");

        assertThat(result)
            .contains("error")
            .containsIgnoringCase("request is required");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> mock(ChatClient.class), null);

        String result = callback.call("not-json");

        assertThat(result)
            .contains("error")
            .containsIgnoringCase("invalid tool input");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentReturnsNull() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"request\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).containsIgnoringCase("returned null");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentThrows() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(new RuntimeException("conversion engine failure"));

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"request\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText())
                .as("payload must surface tool name")
                .contains("importWorkflow failed")
                .as("payload must NOT leak the exception getMessage()")
                .doesNotContain("conversion engine failure");
    }

    @Test
    void testCallForwardsParentToolContextToSubagent() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        Map<String, Object> parentContextMap = Map.of("workspaceId", 11L, "userId", 42L);

        ToolContext parentToolContext = new ToolContext(parentContextMap);

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> chatClient, null);

        callback.call("{\"request\":\"any\"}", parentToolContext);

        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.captor();

        verify(requestSpec).toolContext(contextCaptor.capture());

        assertThat(contextCaptor.getValue())
            .containsAllEntriesOf(parentContextMap)
            .containsKey("bytechef.workflowEditor.persistedWorkflows");
    }

    @Test
    void testCallForwardsEmptyMapWhenParentToolContextIsNull() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> chatClient, null);

        callback.call("{\"request\":\"any\"}", null);

        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.captor();

        verify(requestSpec).toolContext(contextCaptor.capture());

        assertThat(contextCaptor.getValue())
            .containsOnlyKeys("bytechef.workflowEditor.persistedWorkflows");
    }

    @Test
    void testCallResolvesChatClientFromFactoryPerCall() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("converted");

        AtomicInteger factoryCalls = new AtomicInteger();

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> {
            factoryCalls.incrementAndGet();

            return chatClient;
        }, null);

        String firstResult = callback.call("{\"request\":\"convert this n8n workflow\"}");
        String secondResult = callback.call("{\"request\":\"convert that make workflow\"}");

        assertThat(firstResult).isEqualTo("converted");
        assertThat(secondResult).isEqualTo("converted");
        assertThat(factoryCalls.get()).isEqualTo(2);
    }

    @Test
    void testToolDefinitionExposesConverterAgentNameAndRequestSchema() {
        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> mock(ChatClient.class), null);

        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("importWorkflow");
        assertThat(callback.getToolDefinition()
            .inputSchema()).contains("\"request\"");
    }

    @Test
    void testDescriptionTakesAProjectIdForANewWorkflowOrAWorkflowIdForAnExistingOne() {
        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> mock(ChatClient.class), null);

        String description = callback.getToolDefinition()
            .description();

        assertThat(description.replaceAll("\\s+", " "))
            .contains("Pass workflowId to convert into an EXISTING workflow, or projectId to create a BRAND-NEW")
            .contains("never both")
            .doesNotContain("create it with createProjectWorkflow first");
    }

    private static Stream<Arguments> upstreamFailures() {
        return Stream.of(
            Arguments.of(WebClientResponseException.create(400, "Bad Request", null, null, null)),
            Arguments.of(WebClientResponseException.create(503, "Service Unavailable", null, null, null)),
            Arguments.of(new RuntimeException(new IOException("connection reset"))),
            Arguments.of(new RuntimeException(new TimeoutException("upstream timeout"))),
            Arguments.of(new NullPointerException("malformed response")));
    }

    @ParameterizedTest
    @MethodSource("upstreamFailures")
    void testCallSurfacesAllRuntimeExceptionTypesAsToolError(RuntimeException upstreamException) throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(upstreamException);

        ConverterAgentToolCallback callback = new ConverterAgentToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"request\":\"any\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("importWorkflow failed");
    }

    private static void stubToolContext(ChatClientRequestSpec requestSpec) {
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
    }

    private static ChatClient stubbedChatClient(@Nullable String content) {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(content);

        return chatClient;
    }

    @SuppressWarnings("unchecked")
    private static ConverterAgentToolCallback.TargetWorkflowManager newTargetWorkflowManager() {
        ConverterAgentToolCallback.TargetWorkflowManager targetWorkflowManager = mock(
            ConverterAgentToolCallback.TargetWorkflowManager.class);

        when(targetWorkflowManager.createWorkflow(eq(7L), any(ToolContext.class))).thenAnswer(invocation -> {
            ToolContext toolContext = invocation.getArgument(1);

            List<Map<String, Object>> captures = (List<Map<String, Object>>) toolContext.getContext()
                .get("bytechef.workflowEditor.persistedWorkflows");

            captures.add(
                Map.of(
                    "created", true, "workflowId", "wf-new", "projectId", 7L, "projectWorkflowId", 1L, "name",
                    "Imported workflow"));

            return "wf-new";
        });

        return targetWorkflowManager;
    }

    @SuppressWarnings("unchecked")
    private static ChatClient chatClientUpdatingWorkflow(String workflowId, String content) {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenAnswer(invocation -> {
            Map<String, Object> context = invocation.getArgument(0);

            List<Map<String, Object>> captures =
                (List<Map<String, Object>>) context.get("bytechef.workflowEditor.persistedWorkflows");

            captures.add(
                Map.of(
                    "created", false, "workflowId", workflowId, "projectId", 7L, "projectWorkflowId", 1L, "name",
                    "Converted"));

            return requestSpec;
        });
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(content);

        return chatClient;
    }
}
