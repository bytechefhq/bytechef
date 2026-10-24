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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
final class IntelligentToolAskCapabilityTest {

    @Test
    void testOnlyImportWorkflowIsNotAskCapable() {
        List<IntelligentToolDefinition> definitions = definitions();

        assertThat(definitions).hasSize(6);

        assertThat(definitions).filteredOn(definition -> !definition.askCapable())
            .extracting(IntelligentToolDefinition::name)
            .containsExactly("importWorkflow");
    }

    @Test
    void testConverterPromptStillForbidsAsking() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/prompt_converter_build.txt")) {
            assertThat(inputStream).isNotNull();

            String prompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(prompt).contains("You must NEVER ask the user any questions");
        }
    }

    @Test
    void testSubAgentPromptSaysItHasNoConnectionPickers() throws IOException {
        try (InputStream inputStream =
            getClass().getResourceAsStream("/prompt_workflow_editor_build_sub_agent.txt")) {

            assertThat(inputStream).isNotNull();

            String prompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(prompt).contains("You do not have these tools: selectConnection")
                .doesNotContain("askUserQuestion");
        }
    }

    private static List<IntelligentToolDefinition> definitions() {
        IntelligentToolContributor contributor = new CopilotIntelligentToolContributorConfiguration()
            .copilotIntelligentToolContributor(
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(IntelligentToolChatClientFactory.class)),
                present(mock(SubAgentChatModelResolver.class)),
                absent(), absent());

        return contributor.getIntelligentToolDefinitions();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> present(T value) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(value);

        return objectProvider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> absent() {
        return mock(ObjectProvider.class);
    }
}
