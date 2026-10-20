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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.commons.util.JsonUtils;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.ObjectMapper;

/**
 * Pins that the tool definition builds before {@link JsonUtils} has a mapper, as it must while Spring creates the beans
 * that inspect tool definitions at startup.
 *
 * @author Ivica Cardic
 */
class WorkspaceScopedSubAgentToolCallbackStartupTest {

    @Test
    void testToolDefinitionBuildsBeforeJsonUtilsHasAMapper() throws ReflectiveOperationException {
        Field objectMapperField = JsonUtils.class.getDeclaredField("objectMapper");

        objectMapperField.setAccessible(true);

        ObjectMapper previousObjectMapper = (ObjectMapper) objectMapperField.get(null);

        objectMapperField.set(null, null);

        try {
            ToolCallback delegate = mock(ToolCallback.class);

            when(delegate.getToolDefinition()).thenReturn(
                ToolDefinition.builder()
                    .name("importWorkflow")
                    .description("Import a workflow.")
                    .inputSchema(
                        "{\"type\":\"object\",\"properties\":{\"request\":{\"type\":\"string\"}," +
                            "\"projectId\":{\"type\":\"integer\"}},\"required\":[\"request\"]}")
                    .build());

            ToolDefinition toolDefinition = new WorkspaceScopedSubAgentToolCallback(
                delegate, mock(WorkspaceFacade.class)).getToolDefinition();

            assertThat(toolDefinition.inputSchema())
                .contains("\"request\"", "\"projectId\"", "\"workspaceId\"", "\"environment\"", "\"required\"");
        } finally {
            objectMapperField.set(null, previousObjectMapper);
        }
    }
}
