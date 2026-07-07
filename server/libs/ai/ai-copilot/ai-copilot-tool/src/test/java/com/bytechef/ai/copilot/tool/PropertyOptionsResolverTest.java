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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.PropertyOptionsResolver.OptionsLookupResult;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver.OptionsLookupResult.Failure;
import com.bytechef.ai.copilot.tool.PropertyOptionsResolver.OptionsLookupResult.Success;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PropertyOptionsResolverTest {

    private final PropertyOptionsResolver resolver = new PropertyOptionsResolver(
        new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

    @Test
    void testBuildSuccessEnvelopeCarriesOptionsAndTruncatedFlag() {
        Option option = mock(Option.class);

        when(option.getLabel()).thenReturn("#general");
        when(option.getValue()).thenReturn("C123");

        Map<String, Object> envelope =
            resolver.buildSuccessEnvelope("slack", "actionName", "sendMessage", "channel", List.of(option), true);

        assertThat(envelope.get("componentName")).isEqualTo("slack");
        assertThat(envelope.get("actionName")).isEqualTo("sendMessage");
        assertThat(envelope.get("propertyName")).isEqualTo("channel");
        assertThat(envelope.get("truncated")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) envelope.get("options");

        assertThat(options).hasSize(1);

        Map<String, Object> firstOption = options.get(0);

        assertThat(firstOption.get("label")).isEqualTo("#general");
        assertThat(firstOption.get("value")).isEqualTo("C123");
    }

    @Test
    void testBuildSuccessEnvelopeEmitsTruncatedFalse() {
        Map<String, Object> envelope =
            resolver.buildSuccessEnvelope("slack", "actionName", "sendMessage", "channel", List.of(), false);

        assertThat(envelope.containsKey("truncated")).isTrue();
        assertThat(envelope.get("truncated")).isEqualTo(false);
    }

    @Test
    void testDependencyMissingEnvelope() {
        Map<String, Object> envelope = resolver.dependencyMissingEnvelope(List.of("spreadsheetId"));

        assertThat(envelope.get("error")).isEqualTo("dependency_missing");
        assertThat(envelope.get("missing")).isEqualTo(List.of("spreadsheetId"));
        assertThat(envelope.get("hint")).isNotNull();
    }

    @Test
    void testNoOptionsForPropertyEnvelope() {
        assertThat(resolver.noOptionsForPropertyEnvelope()
            .get("error")).isEqualTo("no_options_for_property");
    }

    @Test
    void testConnectionRequiredEnvelope() {
        Map<String, Object> envelope = resolver.connectionRequiredEnvelope("slack");

        assertThat(envelope.get("error")).isEqualTo("connection_required");
        assertThat(envelope.get("componentName")).isEqualTo("slack");
        assertThat(envelope.get("hint")).isNotNull();
    }

    @Test
    void testEntityNotFoundEnvelopeForAction() {
        Map<String, Object> envelope = resolver.entityNotFoundEnvelope(
            "action_not_found", "actionName", "sendMessage", List.of("sendChannelMessage", "sendDirectMessage"));

        assertThat(envelope.get("error")).isEqualTo("action_not_found");
        assertThat(envelope.get("actionName")).isEqualTo("sendMessage");
        assertThat(envelope.get("valid")).isEqualTo(List.of("sendChannelMessage", "sendDirectMessage"));
        assertThat(envelope.get("hint")).asString()
            .contains("valid");
    }

    @Test
    void testEntityNotFoundEnvelopeForProperty() {
        Map<String, Object> envelope = resolver.entityNotFoundEnvelope(
            "property_not_found", "propertyName", "channelId", List.of("channel", "post_at", "text"));

        assertThat(envelope.get("error")).isEqualTo("property_not_found");
        assertThat(envelope.get("propertyName")).isEqualTo("channelId");
        assertThat(envelope.get("valid")).isEqualTo(List.of("channel", "post_at", "text"));
    }

    private static ActionDefinitionService stubActionService(
        String component, int version, String action, String... propertyNames) {

        ActionDefinitionService service = mock(ActionDefinitionService.class);

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

        return service;
    }

    @Test
    void testResolveActionPropertyOptionsReturnsActionNotFound() {
        ActionDefinitionService service = mock(ActionDefinitionService.class);

        ActionDefinition real = mock(ActionDefinition.class);

        when(real.getName()).thenReturn("sendChannelMessage");
        when(service.getActionDefinitions("slack", 1)).thenReturn(List.of(real));

        OptionsLookupResult result = resolver.resolveActionPropertyOptions(
            service, mock(ActionDefinitionFacade.class), null, "slack", 1, "sendMessage", "channel", null, null, null,
            25);

        assertThat(result).isInstanceOf(Failure.class);
        assertThat(((Failure) result).metricTag()).isEqualTo("action_not_found");
        assertThat(((Failure) result).envelope()).containsEntry("error", "action_not_found");
    }

    @Test
    void testResolveActionPropertyOptionsReturnsPropertyNotFound() {
        ActionDefinitionService service = stubActionService("slack", 1, "sendChannelMessage", "channel", "text");

        OptionsLookupResult result = resolver.resolveActionPropertyOptions(
            service, mock(ActionDefinitionFacade.class), null, "slack", 1, "sendChannelMessage", "channelId", null,
            null, null, 25);

        assertThat(((Failure) result).metricTag()).isEqualTo("property_not_found");
    }

    @Test
    void testResolveActionPropertyOptionsReturnsConnectionRequired() {
        ActionDefinitionService service = stubActionService("slack", 1, "sendChannelMessage", "channel");

        when(service.propertyHasOptionsDataSource("slack", 1, "sendChannelMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "sendChannelMessage", "channel")).thenReturn(List.of());
        when(service.actionDefinesConnection("slack", 1, "sendChannelMessage")).thenReturn(true);

        OptionsLookupResult result = resolver.resolveActionPropertyOptions(
            service, mock(ActionDefinitionFacade.class), null, "slack", 1, "sendChannelMessage", "channel", null, null,
            null, 25);

        assertThat(((Failure) result).metricTag()).isEqualTo("connection_required");
    }

    @Test
    void testResolveActionPropertyOptionsReturnsCappedSuccess() {
        ActionDefinitionService service = stubActionService("slack", 1, "sendChannelMessage", "channel");
        ActionDefinitionFacade facade = mock(ActionDefinitionFacade.class);

        when(service.propertyHasOptionsDataSource("slack", 1, "sendChannelMessage", "channel")).thenReturn(true);
        when(service.getPropertyLookupDependsOn("slack", 1, "sendChannelMessage", "channel")).thenReturn(List.of());
        when(service.actionDefinesConnection("slack", 1, "sendChannelMessage")).thenReturn(true);

        List<Option> options = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            Option option = mock(Option.class);

            when(option.getValue()).thenReturn("C" + i);

            options.add(option);
        }

        when(facade.executeOptions(
            org.mockito.ArgumentMatchers.eq("slack"), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.eq("sendChannelMessage"), org.mockito.ArgumentMatchers.eq("channel"),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(7L)))
                .thenReturn(options);

        OptionsLookupResult result = resolver.resolveActionPropertyOptions(
            service, facade, null, "slack", 1, "sendChannelMessage", "channel", null, 7L, null, 25);

        assertThat(result).isInstanceOf(Success.class);
        assertThat(((Success) result).options()).hasSize(25);
        assertThat(((Success) result).truncated()).isTrue();
    }
}
