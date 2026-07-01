/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SelectTriggerPropertyOptionToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    private final PropertyOptionsResolver resolver = new PropertyOptionsResolver(
        new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

    private ToolContext toolContext() {
        return new ToolContext(
            new AgentToolInvocationContext(1L, null, 0L, "thread-1", null).toToolContext());
    }

    private static void stubValidTrigger(
        TriggerDefinitionService service, String component, int version, String trigger, String... propertyNames) {

        TriggerDefinition listed = mock(TriggerDefinition.class);

        when(listed.getName()).thenReturn(trigger);
        when(service.getTriggerDefinitions(component, version)).thenReturn(List.of(listed));

        TriggerDefinition fetched = mock(TriggerDefinition.class);

        List<Property> properties = new ArrayList<>();

        for (String name : propertyNames) {
            Property property = mock(Property.class);

            when(property.getName()).thenReturn(name);

            properties.add(property);
        }

        when(fetched.getProperties()).thenAnswer(invocation -> properties);
        when(service.getTriggerDefinition(component, version, trigger)).thenReturn(fetched);
    }

    @Test
    void testReturnsSelectMarkerWithAllOptionsOnSuccess() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);
        TriggerDefinitionFacade facade = mock(TriggerDefinitionFacade.class);

        stubValidTrigger(service, "slack", 1, "newMessage", "channel");

        when(service.propertyHasOptionsDataSource("slack", 1, "newMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "newMessage", "channel")).thenReturn(List.of());
        when(service.triggerDefinesConnection("slack", 1, "newMessage")).thenReturn(true);

        Option general = mock(Option.class);
        Option random = mock(Option.class);

        when(general.getLabel()).thenReturn("general");
        when(general.getValue()).thenReturn("C06H2PR8LSV");
        when(random.getLabel()).thenReturn("random");
        when(random.getValue()).thenReturn("C06GSJ5RPBN");

        when(facade.executeOptions(eq("slack"), anyInt(), eq("newMessage"), eq("channel"), any(), any(),
            any(), eq(7L))).thenReturn(List.of(general, random));

        SelectTriggerPropertyOptionToolCallback callback = new SelectTriggerPropertyOptionToolCallback(
            service, facade, resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"channel\","
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
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);

        stubValidTrigger(service, "slack", 1, "newMessage", "channel", "text");

        SelectTriggerPropertyOptionToolCallback callback = new SelectTriggerPropertyOptionToolCallback(
            service, mock(TriggerDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"channelId\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("property_not_found");
    }

    @Test
    void testRejectsBlankPropertyName() throws Exception {
        SelectTriggerPropertyOptionToolCallback callback = new SelectTriggerPropertyOptionToolCallback(
            mock(TriggerDefinitionService.class), mock(TriggerDefinitionFacade.class), resolver,
            mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("propertyName is required");
    }
}
