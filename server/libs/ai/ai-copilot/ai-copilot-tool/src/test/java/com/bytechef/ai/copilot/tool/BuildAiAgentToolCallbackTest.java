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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class BuildAiAgentToolCallbackTest {

    private final ChatClient chatClient = mock(ChatClient.class);
    private final ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);
    private final BuildAiAgentToolCallback buildAiAgentToolCallback =
        new BuildAiAgentToolCallback(chatModel -> chatClient, null);

    @Test
    void testCallCreatesTheAgentInTheGivenProject() {
        stubSubAgentReply("built");

        String result = buildAiAgentToolCallback.call(
            "{\"projectId\": 12, \"instruction\": \"An agent called Support Triage.\"}");

        assertThat(result).isEqualTo("built");
        assertThat(requestCaptor.getValue())
            .contains("Create a new AI Agent in project 12")
            .contains("projectId 12")
            .contains("already has an agent with that title, update it")
            .endsWith("An agent called Support Triage.");
    }

    @Test
    void testCallUpdatesTheGivenAgent() {
        stubSubAgentReply("updated");

        String result = buildAiAgentToolCallback.call(
            "{\"aiAgentId\": 34, \"instruction\": \"Add chat memory.\"}");

        assertThat(result).isEqualTo("updated");
        assertThat(requestCaptor.getValue())
            .contains("Update the existing AI Agent with id 34")
            .endsWith("Add chat memory.");
    }

    @Test
    void testCallRequiresAnInstruction() {
        assertThat(buildAiAgentToolCallback.call("{\"projectId\": 12}")).contains("instruction is required");
    }

    @Test
    void testCallRejectsAProjectAndAnAgentTogether() {
        assertThat(buildAiAgentToolCallback.call(
            "{\"projectId\": 12, \"aiAgentId\": 34, \"instruction\": \"Add chat memory.\"}"))
                .contains("either projectId or aiAgentId");
    }

    private void stubSubAgentReply(String reply) {
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(requestCaptor.capture())).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(reply);
    }
}
