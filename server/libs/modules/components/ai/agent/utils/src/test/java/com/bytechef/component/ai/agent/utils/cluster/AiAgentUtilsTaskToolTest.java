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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallEvent;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentUtilsTaskToolTest {

    @Test
    void testTaskToolNeverConstructsLibraryDefinedSubagents() throws IOException {
        String source = Files.readString(
            Path.of("src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsTaskTool.java"));

        assertThat(source).doesNotContain("ClaudeSubagentType");
        assertThat(source).doesNotContain("ShellTools");
        assertThat(source).doesNotContain("FileSystemTools");
    }

    @Test
    void testSubagentToolCallsAreReported() throws Exception {
        AiAgentToolFacade aiAgentToolFacade = mock(AiAgentToolFacade.class);
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        when(clusterElementDefinitionService.<MultipleConnectionsToolFunction>getClusterElement(
            eq("httpClient"), eq(1), eq("get")))
                .thenReturn(mock(MultipleConnectionsToolFunction.class));
        when(aiAgentToolFacade.getFunctionToolCallback(any(ClusterElement.class), anyMap(), any(), any()))
            .thenReturn(namedToolCallback("get"));

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getJobId()).thenReturn(63335L);
        when(actionContext.getTaskExecutionId()).thenReturn(633351L);

        ClusterElement subagentClusterElement = new ClusterElement(
            null, null,
            Map.of(
                "clusterElements",
                Map.of("tools", List.of(Map.of("name", "httpClient_9", "type", "httpClient/v1/get", "parameters",
                    Map.of())))),
            null, "aiAgentUtils/v1/subagentTool", Map.of("subagentName", "Analyst"), "subagent_1");

        List<ClusterElementCallEvent> events = new CopyOnWriteArrayList<>();

        AutoCloseable handle = ClusterElementCallRecorder.getInstance()
            .addListener(63335L, events::add);

        try {
            List<ToolCallback> toolCallbacks = new AiAgentUtilsTaskTool(
                aiAgentToolFacade, clusterElementDefinitionService)
                    .buildSubagentToolCallbacks(subagentClusterElement, Map.of(), actionContext);

            toolCallbacks.getFirst()
                .call("{}");
        } finally {
            handle.close();

            ClusterElementCallRecorder.getInstance()
                .drain(633351L);
        }

        assertThat(events).extracting(ClusterElementCallEvent::type)
            .containsExactly(ClusterElementCallEvent.Type.STARTED, ClusterElementCallEvent.Type.COMPLETED);
        assertThat(events).extracting(event -> event.call()
            .workflowNodeName())
            .containsOnly("httpClient_9");
        assertThat(events).extracting(event -> event.record()
            .componentName())
            .containsOnly("httpClient");
    }

    @Test
    void testSubagentModelFailureIsRecordedOnTheSubagentModelElement() throws Exception {
        AiAgentToolFacade aiAgentToolFacade = mock(AiAgentToolFacade.class);
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModelFunction modelFunction =
            (inputParameters, connectionParameters, responseFormatRequired) -> new FailingChatModel();

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(eq("openAi"), eq(1), eq("model")))
            .thenReturn(modelFunction);

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getJobId()).thenReturn(63336L);
        when(actionContext.getTaskExecutionId()).thenReturn(633361L);

        Map<String, Object> subagentElementMap = Map.of(
            "name", "subagent_1", "type", "aiAgentUtils/v1/subagentTool", "parameters",
            Map.of("subagentName", "Analyst", "description", "Analyses things", "instructions", "Analyse carefully"),
            "clusterElements",
            Map.of("model", Map.of("name", "openAi_3", "type", "openAi/v1/model", "parameters", Map.of())));

        Parameters extensions = MockParametersFactory.create(
            Map.of("clusterElements", Map.of("subagent", List.of(subagentElementMap))));

        AiAgentUtilsTaskTool aiAgentUtilsTaskTool = new AiAgentUtilsTaskTool(
            aiAgentToolFacade, clusterElementDefinitionService);

        MultipleConnectionsToolCallbackProviderFunction toolCallbackProviderFunction =
            aiAgentUtilsTaskTool.clusterElementDefinition.getElement();

        ToolCallbackProvider toolCallbackProvider = toolCallbackProviderFunction.apply(
            MockParametersFactory.create(Map.of()), MockParametersFactory.create(Map.of()), extensions, Map.of(),
            actionContext);

        ToolCallback taskToolCallback = toolCallbackProvider.getToolCallbacks()[0];

        assertThatThrownBy(() -> taskToolCallback.call(
            "{\"description\":\"Analyse\",\"prompt\":\"Analyse the thing\",\"subagent_type\":\"Analyst\"}"))
                .hasMessageContaining("model unavailable");

        List<ClusterElementCallRecord> records = ClusterElementCallRecorder.getInstance()
            .drain(633361L)
            .records();

        ClusterElementCallRecord taskCallRecord = records.stream()
            .filter(callRecord -> "subagent_1".equals(callRecord.workflowNodeName()))
            .findFirst()
            .orElseThrow();

        List<ClusterElementCallRecord> modelRecords = records.stream()
            .filter(callRecord -> "openAi_3".equals(callRecord.workflowNodeName()))
            .toList();

        assertThat(taskCallRecord.status()).isEqualTo(ClusterElementCallRecord.Status.FAILED);
        assertThat(modelRecords).hasSize(1);
        assertThat(modelRecords.getFirst()
            .status()).isEqualTo(ClusterElementCallRecord.Status.FAILED);
        assertThat(modelRecords.getFirst()
            .componentName()).isEqualTo("openAi");
        assertThat(modelRecords.getFirst()
            .parentId()).isEqualTo(taskCallRecord.id());
    }

    private static ToolCallback namedToolCallback(String toolName) {
        return new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                    .name(toolName)
                    .description("Returns a fixed value")
                    .inputSchema("{\"type\":\"object\"}")
                    .build();
            }

            @Override
            public String call(String toolInput) {
                return "called";
            }
        };
    }

    private static final class FailingChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new IllegalStateException("model unavailable");
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return ChatOptions.builder()
                .build();
        }
    }
}
