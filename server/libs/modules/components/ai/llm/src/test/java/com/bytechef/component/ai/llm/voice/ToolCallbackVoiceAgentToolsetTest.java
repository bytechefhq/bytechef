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

package com.bytechef.component.ai.llm.voice;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.definition.voice.VoiceAgentToolset;
import com.bytechef.platform.component.definition.voice.VoiceToolDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class ToolCallbackVoiceAgentToolsetTest {

    @Test
    void testDefinitionsMirrorTheCallbacks() {
        VoiceAgentToolset toolset =
            new ToolCallbackVoiceAgentToolset(List.of(callback("lookupOrder", "Finds an order")));

        assertThat(toolset.definitions())
            .containsExactly(new VoiceToolDefinition("lookupOrder", "Finds an order", "{\"type\":\"object\"}"));
    }

    @Test
    void testCallRoutesByNameAndReturnsTheResult() {
        VoiceAgentToolset toolset =
            new ToolCallbackVoiceAgentToolset(List.of(callback("lookupOrder", "Finds an order")));

        assertThat(toolset.call("lookupOrder", "{\"id\":\"4411\"}")).isEqualTo("result for {\"id\":\"4411\"}");
    }

    @Test
    void testUnknownToolAndFailingToolNeverThrow() {
        ToolCallback failing = new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                    .name("boom")
                    .description("fails")
                    .inputSchema("{\"type\":\"object\"}")
                    .build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("provider down");
            }
        };

        VoiceAgentToolset toolset = new ToolCallbackVoiceAgentToolset(List.of(failing));

        assertThat(toolset.call("nope", "{}")).isEqualTo(VoiceAgentToolset.unknownTool("nope"));
        assertThat(toolset.call("boom", "{}")).isEqualTo("Tool failed: provider down");
    }

    @Test
    void testDuplicateToolNamesKeepTheFirstCallback() {
        VoiceAgentToolset toolset = new ToolCallbackVoiceAgentToolset(
            List.of(answering("lookupOrder", "first", "from first"),
                answering("lookupOrder", "second", "from second")));

        assertThat(toolset.definitions())
            .singleElement()
            .extracting(VoiceToolDefinition::description)
            .isEqualTo("first");
        assertThat(toolset.call("lookupOrder", "{}")).isEqualTo("from first");
    }

    @Test
    void testFailureWithoutAMessageNamesTheExceptionType() {
        VoiceAgentToolset toolset = new ToolCallbackVoiceAgentToolset(
            List.of(failingWith("npe", new NullPointerException())));

        assertThat(toolset.call("npe", "{}")).isEqualTo("Tool failed: NullPointerException");
    }

    private static ToolCallback answering(String name, String description, String result) {
        return new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                    .name(name)
                    .description(description)
                    .inputSchema("{\"type\":\"object\"}")
                    .build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    private static ToolCallback callback(String name, String description) {
        return new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                    .name(name)
                    .description(description)
                    .inputSchema("{\"type\":\"object\"}")
                    .build();
            }

            @Override
            public String call(String toolInput) {
                return "result for " + toolInput;
            }
        };
    }

    private static ToolCallback failingWith(String name, RuntimeException exception) {
        return new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                    .name(name)
                    .description("fails")
                    .inputSchema("{\"type\":\"object\"}")
                    .build();
            }

            @Override
            public String call(String toolInput) {
                throw exception;
            }
        };
    }
}
