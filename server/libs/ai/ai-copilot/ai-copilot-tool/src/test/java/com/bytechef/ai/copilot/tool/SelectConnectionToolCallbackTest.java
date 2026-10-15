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

import com.bytechef.platform.ai.tool.util.AllowedComponentNamesUtils;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SelectConnectionToolCallbackTest {

    private static final ToolContext SLACK_ONLY_TOOL_CONTEXT = new ToolContext(
        Map.of(AllowedComponentNamesUtils.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack")));

    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final JsonMapper jsonMapper = new JsonMapper();
    private final SelectConnectionToolCallback callback = new SelectConnectionToolCallback(componentDefinitionService);

    @Test
    void testReturnsSelectConnectionMarkerThatWaitsForTheUser() throws Exception {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getTitle()).thenReturn("Slack");
        when(componentDefinitionService.fetchComponentDefinition(eq("slack"), any()))
            .thenReturn(Optional.of(componentDefinition));

        JsonNode node = jsonMapper.readTree(callback.call("{\"componentName\":\"slack\"}", SLACK_ONLY_TOOL_CONTEXT));

        assertThat(node.get("kind")
            .asString()).isEqualTo("select-connection");
        assertThat(node.get("componentLabel")
            .asString()).isEqualTo("Slack");
        assertThat(node.get("awaitingAnswer")
            .asBoolean()).isTrue();
    }

    @Test
    void testRejectsAComponentOutsideTheAllowList() {
        when(componentDefinitionService.fetchComponentDefinition(eq("hubspot"), any()))
            .thenReturn(Optional.of(mock(ComponentDefinition.class)));

        String result = callback.call("{\"componentName\":\"hubspot\"}", SLACK_ONLY_TOOL_CONTEXT);

        assertThat(result).contains("This chat can't use hubspot");
    }
}
