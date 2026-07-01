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
class LookupTriggerPropertyOptionsToolCallbackTest {

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
    void testReturnsNoOptionsWhenPropertyHasNoDataSource() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);

        stubValidTrigger(service, "slack", 1, "newMessage", "text");
        when(service.propertyHasOptionsDataSource("slack", 1, "newMessage", "text")).thenReturn(false);

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, mock(TriggerDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"text\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("no_options_for_property");
    }

    @Test
    void testReturnsDependencyMissingWhenSiblingAbsent() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);

        stubValidTrigger(service, "googleSheets", 1, "newRow", "sheetName");
        when(service.propertyHasOptionsDataSource("googleSheets", 1, "newRow", "sheetName")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("googleSheets", 1, "newRow", "sheetName"))
            .thenReturn(List.of("spreadsheetId"));

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, mock(TriggerDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"googleSheets\",\"triggerName\":\"newRow\",\"propertyName\":\"sheetName\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("dependency_missing");
        assertThat(node.get("missing")
            .get(0)
            .asText()).isEqualTo("spreadsheetId");
    }

    @Test
    void testReturnsConnectionRequiredWhenConnectionMissing() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);

        stubValidTrigger(service, "slack", 1, "newMessage", "channel");
        when(service.propertyHasOptionsDataSource("slack", 1, "newMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "newMessage", "channel")).thenReturn(List.of());
        when(service.triggerDefinesConnection("slack", 1, "newMessage")).thenReturn(true);

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, mock(TriggerDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"channel\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("connection_required");
    }

    @Test
    void testReturnsCappedOptionsAndTruncatedFlagOnSuccess() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);
        TriggerDefinitionFacade facade = mock(TriggerDefinitionFacade.class);

        stubValidTrigger(service, "slack", 1, "newMessage", "channel");
        when(service.propertyHasOptionsDataSource("slack", 1, "newMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "newMessage", "channel")).thenReturn(List.of());
        when(service.triggerDefinesConnection("slack", 1, "newMessage")).thenReturn(true);

        List<Option> options = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            Option option = mock(Option.class);

            when(option.getLabel()).thenReturn("#channel" + i);
            when(option.getValue()).thenReturn("C" + i);

            options.add(option);
        }

        when(facade.executeOptions(
            eq("slack"), anyInt(), eq("newMessage"), eq("channel"), any(), any(), any(), eq(42L)))
                .thenReturn(options);

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, facade, resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"channel\","
                + "\"connectionId\":42}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("triggerName")
            .asText()).isEqualTo("newMessage");
        assertThat(node.get("options")
            .size()).isEqualTo(25);
        assertThat(node.get("truncated")
            .asBoolean()).isTrue();
    }

    @Test
    void testReturnsTriggerNotFoundWithValidNames() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);

        TriggerDefinition real = mock(TriggerDefinition.class);

        when(real.getName()).thenReturn("newMessage");
        when(service.getTriggerDefinitions("slack", 1)).thenReturn(List.of(real));

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, mock(TriggerDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"onMessage\",\"propertyName\":\"channel\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("trigger_not_found");
        assertThat(node.get("valid")
            .get(0)
            .asText()).isEqualTo("newMessage");
    }

    @Test
    void testReturnsPropertyNotFoundWithValidNames() throws Exception {
        TriggerDefinitionService service = mock(TriggerDefinitionService.class);

        stubValidTrigger(service, "slack", 1, "newMessage", "channel", "text");

        LookupTriggerPropertyOptionsToolCallback callback = new LookupTriggerPropertyOptionsToolCallback(
            service, mock(TriggerDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"triggerName\":\"newMessage\",\"propertyName\":\"channelId\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("property_not_found");

        List<String> validNames = new ArrayList<>();

        node.get("valid")
            .forEach(name -> validNames.add(name.asText()));

        assertThat(validNames).contains("channel");
    }
}
