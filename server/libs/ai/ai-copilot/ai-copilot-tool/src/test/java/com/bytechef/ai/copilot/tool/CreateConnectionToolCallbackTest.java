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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CreateConnectionToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();
    private ComponentDefinitionService componentDefinitionService;
    private CreateConnectionToolCallback callback;

    @BeforeEach
    void setUp() {
        componentDefinitionService = mock(ComponentDefinitionService.class);
        callback = new CreateConnectionToolCallback(componentDefinitionService);
    }

    @Test
    void testToolDefinitionExposesCreateConnectionName() {
        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("createConnection");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("componentName");
    }

    @Test
    void testCallHappyPathResolvesComponentLabel() throws Exception {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getTitle()).thenReturn("Slack");
        when(componentDefinitionService.fetchComponentDefinition(eq("slack"), any()))
            .thenReturn(Optional.of(componentDefinition));

        String result = callback.call("{\"componentName\":\"slack\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("kind")
            .asText()).isEqualTo("create-connection");
        assertThat(node.get("componentName")
            .asText()).isEqualTo("slack");
        assertThat(node.get("componentLabel")
            .asText()).isEqualTo("Slack");
        assertThat(node.has("suggestedName")).isFalse();
    }

    @Test
    void testCallUnknownComponentReturnsErrorWithSuggestions() throws Exception {
        ComponentDefinition googleMail = mock(ComponentDefinition.class);

        when(googleMail.getName()).thenReturn("googleMail");
        when(googleMail.getTitle()).thenReturn("Gmail");

        when(componentDefinitionService.fetchComponentDefinition(eq("gmail"), any()))
            .thenReturn(Optional.empty());
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(googleMail));

        String result = callback.call("{\"componentName\":\"gmail\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("kind")).isFalse();
        assertThat(node.has("error")).isTrue();

        String error = node.get("error")
            .asText();

        assertThat(error).contains("gmail");
        assertThat(error).contains("googleMail (Gmail)");
    }

    @Test
    void testCallBlankComponentNameReturnsError() throws Exception {
        String result = callback.call("{\"componentName\":\"\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }
}
