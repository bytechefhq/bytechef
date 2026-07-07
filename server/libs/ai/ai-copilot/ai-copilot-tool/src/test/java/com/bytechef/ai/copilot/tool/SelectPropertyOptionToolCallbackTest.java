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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SelectPropertyOptionToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    private final PropertyOptionsResolver resolver = new PropertyOptionsResolver(
        new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

    private ToolContext toolContext() {
        return new ToolContext(
            new AgentToolInvocationContext(1L, null, 0L, "thread-1", null).toToolContext());
    }

    private static void stubValidAction(
        ActionDefinitionService service, String component, int version, String action, String... propertyNames) {

        ActionDefinition listed = mock(ActionDefinition.class);

        when(listed.getName()).thenReturn(action);
        when(service.getActionDefinitions(component, version)).thenReturn(List.of(listed));

        ActionDefinition fetched = mock(ActionDefinition.class);

        List<Property> properties = new ArrayList<>();

        for (String name : propertyNames) {
            Property property = mock(Property.class);

            when(property.getName()).thenReturn(name);

            properties.add(property);
        }

        when(fetched.getProperties()).thenAnswer(invocation -> properties);
        when(service.getActionDefinition(component, version, action)).thenReturn(fetched);
    }

    @Test
    void testReturnsSelectMarkerWithAllOptionsOnSuccess() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);
        ActionDefinitionFacade facade = mock(ActionDefinitionFacade.class);

        stubValidAction(service, "slack", 1, "sendChannelMessage", "channel");

        when(service.propertyHasOptionsDataSource("slack", 1, "sendChannelMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "sendChannelMessage", "channel")).thenReturn(List.of());
        when(service.actionDefinesConnection("slack", 1, "sendChannelMessage")).thenReturn(true);

        Option general = mock(Option.class);
        Option random = mock(Option.class);

        when(general.getLabel()).thenReturn("general");
        when(general.getValue()).thenReturn("C06H2PR8LSV");
        when(random.getLabel()).thenReturn("random");
        when(random.getValue()).thenReturn("C06GSJ5RPBN");

        when(facade.executeOptions(eq("slack"), anyInt(), eq("sendChannelMessage"), eq("channel"), any(), any(),
            any(), eq(7L))).thenReturn(List.of(general, random));

        SelectPropertyOptionToolCallback callback = new SelectPropertyOptionToolCallback(
            service, facade, resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\",\"propertyName\":\"channel\","
                + "\"connectionId\":7}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("kind")
            .asString()).isEqualTo("select-property-option");
        assertThat(node.get("componentName")
            .asString()).isEqualTo("slack");
        assertThat(node.get("propertyName")
            .asString()).isEqualTo("channel");
        assertThat(node.get("options")
            .size()).isEqualTo(2);
        assertThat(node.get("options")
            .get(0)
            .get("label")
            .asString()).isEqualTo("general");
        assertThat(node.get("options")
            .get(0)
            .get("value")
            .asString()).isEqualTo("C06H2PR8LSV");
        assertThat(node.get("truncated")
            .asBoolean()).isFalse();
    }

    @Test
    void testReturnsPropertyNotFoundEnvelope() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        stubValidAction(service, "slack", 1, "sendChannelMessage", "channel", "text");

        SelectPropertyOptionToolCallback callback = new SelectPropertyOptionToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\",\"propertyName\":\"channelId\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("property_not_found");
    }

    @Test
    void testRejectsBlankPropertyName() throws Exception {
        SelectPropertyOptionToolCallback callback = new SelectPropertyOptionToolCallback(
            mock(ActionDefinitionService.class), mock(ActionDefinitionFacade.class), resolver,
            mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\",\"propertyName\":\"\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("propertyName is required");
    }
}
