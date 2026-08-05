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

package com.bytechef.platform.ai.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;

class AutoMemoryToolsAdvisorTest {

    private final AutoMemoryTools autoMemoryTools = new AutoMemoryTools(
        (relativePath, toolContext) -> {
            throw new UnsupportedOperationException();
        },
        new AutoMemoryDirectoryOps() {
            @Override
            public String list(String path, org.springframework.ai.chat.model.ToolContext toolContext) {
                return "";
            }

            @Override
            public boolean exists(String relativePath, org.springframework.ai.chat.model.ToolContext toolContext) {
                return false;
            }

            @Override
            public void delete(String relativePath, org.springframework.ai.chat.model.ToolContext toolContext) {
            }

            @Override
            public void rename(
                String oldRelativePath, String newRelativePath,
                org.springframework.ai.chat.model.ToolContext toolContext) {
            }
        });

    @Test
    void testBeforeAugmentsSystemPromptAndAddsToolCallbacks() {
        AutoMemoryToolsAdvisor advisor = AutoMemoryToolsAdvisor.builder()
            .autoMemoryTools(autoMemoryTools)
            .memorySystemPrompt(new ByteArrayResource("REMEMBER THINGS".getBytes(StandardCharsets.UTF_8)))
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(
                List.of(new SystemMessage("base system")),
                ToolCallingChatOptions.builder()
                    .build()))
            .build();

        ChatClientRequest result = advisor.before(request, null);

        String systemText = result.prompt()
            .getSystemMessage()
            .getText();

        assertThat(systemText).contains("base system");
        assertThat(systemText).contains("REMEMBER THINGS");

        ToolCallingChatOptions toolCallingChatOptions = (ToolCallingChatOptions) Objects.requireNonNull(
            result.prompt()
                .getOptions(),
            "options");

        List<ToolCallback> toolCallbacks = toolCallingChatOptions.getToolCallbacks();

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .contains("MemoryView", "MemoryCreate", "MemoryStrReplace", "MemoryInsert", "MemoryDelete", "MemoryRename");
    }
}
