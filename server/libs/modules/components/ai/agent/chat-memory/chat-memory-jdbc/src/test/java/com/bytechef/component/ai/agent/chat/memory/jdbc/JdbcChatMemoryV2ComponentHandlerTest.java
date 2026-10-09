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

package com.bytechef.component.ai.agent.chat.memory.jdbc;

import static com.bytechef.platform.component.definition.ai.agent.DataSourceFunction.DATA_SOURCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.JdbcSessionChatMemoryComponentDefinition;
import com.bytechef.platform.component.definition.SessionChatMemoryComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class JdbcChatMemoryV2ComponentHandlerTest {

    @Test
    void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/jdbc-chat-memory_v2.json", createComponentHandler().getDefinition());
    }

    @Test
    void testIsVersionTwoOfJdbcChatMemoryWithADataSourceAndAnOptionalSummarizerModel() {
        ComponentDefinition componentDefinition = createComponentHandler().getDefinition();

        assertThat(componentDefinition.getName()).isEqualTo("jdbcChatMemory");
        assertThat(componentDefinition.getVersion()).isEqualTo(2);

        JdbcSessionChatMemoryComponentDefinition jdbcSessionChatMemoryComponentDefinition =
            (JdbcSessionChatMemoryComponentDefinition) componentDefinition;

        assertThat(jdbcSessionChatMemoryComponentDefinition.getClusterElementTypes())
            .containsExactly(DATA_SOURCE, SessionChatMemoryComponentDefinition.SUMMARIZER_MODEL);
        assertThat(jdbcSessionChatMemoryComponentDefinition.getActionClusterElementTypes()).isEqualTo(
            Map.of(
                "addMessages", List.of(DATA_SOURCE.name()), "getMessages", List.of(DATA_SOURCE.name()),
                "deleteConversation", List.of(DATA_SOURCE.name()), "listConversations", List.of(DATA_SOURCE.name())));
    }

    private static JdbcChatMemoryV2ComponentHandler createComponentHandler() {
        return new JdbcChatMemoryV2ComponentHandler(mock(ClusterElementDefinitionService.class));
    }
}
