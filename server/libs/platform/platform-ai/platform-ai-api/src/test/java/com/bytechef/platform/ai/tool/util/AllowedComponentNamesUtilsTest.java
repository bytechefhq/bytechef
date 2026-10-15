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

package com.bytechef.platform.ai.tool.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AllowedComponentNamesUtilsTest {

    private static final ToolContext SLACK_ONLY_TOOL_CONTEXT = new ToolContext(
        Map.of(AllowedComponentNamesUtils.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack")));

    @Test
    void testGetAllowedComponentNamesTreatsMissingOrEmptyAllowListAsUnrestricted() {
        assertThat(AllowedComponentNamesUtils.getAllowedComponentNames(null)).isNull();
        assertThat(AllowedComponentNamesUtils.getAllowedComponentNames(new ToolContext(Map.of()))).isNull();
        assertThat(
            AllowedComponentNamesUtils.getAllowedComponentNames(
                new ToolContext(
                    Map.of(AllowedComponentNamesUtils.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of()))))
                        .isNull();
    }

    @Test
    void testIsAllowed() {
        assertThat(AllowedComponentNamesUtils.isAllowed("slack", SLACK_ONLY_TOOL_CONTEXT)).isTrue();
        assertThat(AllowedComponentNamesUtils.isAllowed("hubspot", SLACK_ONLY_TOOL_CONTEXT)).isFalse();
        assertThat(AllowedComponentNamesUtils.isAllowed("hubspot", null)).isTrue();
    }

    @Test
    void testGetDisallowedComponentNamesFindsVersionedAndUnversionedTypes() {
        ComponentDefinitionService componentDefinitionService = mockComponentDefinitionService();

        String definition = """
            {"tasks": [
              {"name": "a", "type": "hubspot/v1/createContact"},
              {"name": "b", "type": "salesforce/1/createRecord"},
              {"name": "c", "type": "slack/v1/sendMessage"}
            ]}
            """;

        List<String> disallowedComponentNames = AllowedComponentNamesUtils.getDisallowedComponentNames(
            definition, SLACK_ONLY_TOOL_CONTEXT, componentDefinitionService);

        assertThat(disallowedComponentNames).containsExactly("hubspot", "salesforce");
    }

    @Test
    void testGetDisallowedComponentNamesLetsThroughComponentsWithoutConnectionAndUnknownOnes() {
        ComponentDefinitionService componentDefinitionService = mockComponentDefinitionService();

        String definition = """
            {"tasks": [
              {"name": "a", "type": "logger/v1/info"},
              {"name": "b", "type": "unknownComponent/v1/doIt"},
              {"name": "c", "parameters": {"contentType": {"type": "application/json"}}}
            ]}
            """;

        List<String> disallowedComponentNames = AllowedComponentNamesUtils.getDisallowedComponentNames(
            definition, SLACK_ONLY_TOOL_CONTEXT, componentDefinitionService);

        assertThat(disallowedComponentNames).isEmpty();
    }

    private static ComponentDefinitionService mockComponentDefinitionService() {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);

        when(componentDefinitionService.fetchComponentDefinition(any(), any())).thenReturn(Optional.empty());

        for (String componentName : List.of("hubspot", "salesforce")) {
            ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

            when(componentDefinition.isConnectionRequired()).thenReturn(true);
            when(componentDefinitionService.fetchComponentDefinition(eq(componentName), any()))
                .thenReturn(Optional.of(componentDefinition));
        }

        ComponentDefinition loggerComponentDefinition = mock(ComponentDefinition.class);

        when(loggerComponentDefinition.isConnectionRequired()).thenReturn(false);
        when(componentDefinitionService.fetchComponentDefinition(eq("logger"), any()))
            .thenReturn(Optional.of(loggerComponentDefinition));

        return componentDefinitionService;
    }
}
