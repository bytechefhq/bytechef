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
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
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
class LookupActionPropertyOptionsToolCallbackTest {

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
    void testReturnsNoOptionsWhenPropertyHasNoDataSource() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        stubValidAction(service, "slack", 1, "sendMessage", "text");
        when(service.propertyHasOptionsDataSource("slack", 1, "sendMessage", "text")).thenReturn(false);

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"propertyName\":\"text\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("no_options_for_property");
    }

    @Test
    void testReturnsDependencyMissingWhenSiblingAbsent() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        stubValidAction(service, "googleSheets", 1, "appendRow", "sheetName");
        when(service.propertyHasOptionsDataSource("googleSheets", 1, "appendRow", "sheetName")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("googleSheets", 1, "appendRow", "sheetName"))
            .thenReturn(List.of("spreadsheetId"));

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"googleSheets\",\"actionName\":\"appendRow\",\"propertyName\":\"sheetName\"}",
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
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        stubValidAction(service, "slack", 1, "sendMessage", "channel");
        when(service.propertyHasOptionsDataSource("slack", 1, "sendMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "sendMessage", "channel")).thenReturn(List.of());
        when(service.actionDefinesConnection("slack", 1, "sendMessage")).thenReturn(true);

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"propertyName\":\"channel\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("connection_required");
    }

    @Test
    void testReturnsCappedOptionsAndTruncatedFlagOnSuccess() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);
        ActionDefinitionFacade facade = mock(ActionDefinitionFacade.class);

        stubValidAction(service, "slack", 1, "sendMessage", "channel");
        when(service.propertyHasOptionsDataSource("slack", 1, "sendMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "sendMessage", "channel")).thenReturn(List.of());
        when(service.actionDefinesConnection("slack", 1, "sendMessage")).thenReturn(true);

        List<Option> options = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            Option option = mock(Option.class);

            when(option.getLabel()).thenReturn("#channel" + i);
            when(option.getValue()).thenReturn("C" + i);

            options.add(option);
        }

        when(facade.executeOptions(
            eq("slack"), anyInt(), eq("sendMessage"), eq("channel"), any(), any(), any(), eq(42L)))
                .thenReturn(options);

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, facade, resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"propertyName\":\"channel\","
                + "\"connectionId\":42}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("componentName")
            .asText()).isEqualTo("slack");
        assertThat(node.get("actionName")
            .asText()).isEqualTo("sendMessage");
        assertThat(node.get("options")
            .size()).isEqualTo(25);
        assertThat(node.get("truncated")
            .asBoolean()).isTrue();
        assertThat(node.get("options")
            .get(0)
            .get("value")
            .asText()).isEqualTo("C0");
    }

    @Test
    void testAcceptsDottedDependsOnSatisfiedByLastSegment() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);
        ActionDefinitionFacade facade = mock(ActionDefinitionFacade.class);

        stubValidAction(service, "hubspot", 1, "createDeal", "pipelineStage");
        when(service.propertyHasOptionsDataSource("hubspot", 1, "createDeal", "pipelineStage")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("hubspot", 1, "createDeal", "pipelineStage"))
            .thenReturn(List.of("properties.pipeline"));
        when(service.actionDefinesConnection("hubspot", 1, "createDeal")).thenReturn(true);

        Option option = mock(Option.class);

        when(option.getLabel()).thenReturn("Appointment Scheduled");
        when(option.getValue()).thenReturn("stage1");

        when(facade.executeOptions(
            eq("hubspot"), anyInt(), eq("createDeal"), eq("pipelineStage"), any(), any(), any(), eq(7L)))
                .thenReturn(List.of(option));

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, facade, resolver, mock(ToolStateVisibilityMetrics.class));

        // The dependsOn path is dotted ("properties.pipeline") but the value is supplied under its last segment
        // ("pipeline") — the gate must treat the dependency as satisfied rather than emitting dependency_missing.
        String result = callback.call(
            "{\"componentName\":\"hubspot\",\"actionName\":\"createDeal\",\"propertyName\":\"pipelineStage\","
                + "\"connectionId\":7,\"inputParameters\":{\"pipeline\":\"default\"}}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isFalse();
        assertThat(node.get("options")
            .size()).isEqualTo(1);
    }

    @Test
    void testRejectsBlankPropertyName() throws Exception {
        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            mock(ActionDefinitionService.class), mock(ActionDefinitionFacade.class), resolver,
            mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"propertyName\":\"\"}", toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).contains("propertyName is required");
    }

    @Test
    void testReturnsActionNotFoundWithValidNames() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        ActionDefinition real = mock(ActionDefinition.class);

        when(real.getName()).thenReturn("sendChannelMessage");
        when(service.getActionDefinitions("slack", 1)).thenReturn(List.of(real));

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"propertyName\":\"channel\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText()).isEqualTo("action_not_found");
        assertThat(node.get("valid")
            .get(0)
            .asText()).isEqualTo("sendChannelMessage");
    }

    @Test
    void testReturnsPropertyNotFoundWithValidNames() throws Exception {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        stubValidAction(service, "slack", 1, "sendChannelMessage", "channel", "post_at", "text");

        LookupActionPropertyOptionsToolCallback callback = new LookupActionPropertyOptionsToolCallback(
            service, mock(ActionDefinitionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\",\"propertyName\":\"channelId\"}",
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
