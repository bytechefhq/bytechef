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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Covers the kind-keyed dispatch of the unified lookup tool: ACTION routes to the action resolver path, TRIGGER to
 * the trigger path, and an invalid kind is rejected before any resolution happens.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class LookupComponentPropertyOptionsToolCallbackTest {

    private ActionDefinitionService actionDefinitionService;
    private ActionDefinitionFacade actionDefinitionFacade;
    private TriggerDefinitionService triggerDefinitionService;
    private TriggerDefinitionFacade triggerDefinitionFacade;
    private PropertyOptionsResolver resolver;
    private ToolStateVisibilityMetrics metrics;
    private LookupComponentPropertyOptionsToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        actionDefinitionService = mock(ActionDefinitionService.class);
        actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        triggerDefinitionService = mock(TriggerDefinitionService.class);
        triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);
        resolver = mock(PropertyOptionsResolver.class);
        metrics = mock(ToolStateVisibilityMetrics.class);
        toolCallback = new LookupComponentPropertyOptionsToolCallback(
            actionDefinitionService, actionDefinitionFacade, triggerDefinitionService, triggerDefinitionFacade,
            resolver, metrics);
    }

    @Test
    void testToolDefinitionName() {
        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("lookupPropertyOptions");
    }

    @Test
    void testInvalidKindReturnsErrorWithoutResolving() {
        String result = toolCallback.call(
            "{\"kind\": \"WEBHOOK\", \"componentName\": \"slack\", \"operationName\": \"send\", "
                + "\"propertyName\": \"channel\"}",
            toolContext());

        assertThat(result).contains("error");
        assertThat(result).contains("kind is required and must be ACTION or TRIGGER");

        verify(resolver, never()).resolveActionPropertyOptions(
            any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt());
        verify(resolver, never()).resolveTriggerPropertyOptions(
            any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void testActionKindRoutesToActionResolver() {
        Option option = mock(Option.class);

        when(option.getLabel()).thenReturn("General");
        when(option.getValue()).thenReturn("C123");

        when(resolver.resolveActionPropertyOptions(
            eq(actionDefinitionService), eq(actionDefinitionFacade), any(), eq("slack"), eq(1),
            eq("sendChannelMessage"), eq("channel"), any(), any(), any(), anyInt()))
                .thenReturn(new PropertyOptionsResolver.OptionsLookupResult.Success(List.of(option), false));
        when(resolver.buildSuccessEnvelope(
            eq("slack"), eq("actionName"), eq("sendChannelMessage"), eq("channel"), any(), eq(false)))
                .thenReturn(Map.of("options", List.of()));

        String result = toolCallback.call(
            "{\"kind\": \"ACTION\", \"componentName\": \"slack\", \"operationName\": \"sendChannelMessage\", "
                + "\"propertyName\": \"channel\"}",
            toolContext());

        assertThat(result).contains("options");

        verify(resolver, never()).resolveTriggerPropertyOptions(
            any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void testTriggerKindRoutesToTriggerResolver() {
        Option option = mock(Option.class);

        when(option.getLabel()).thenReturn("Sheet1");
        when(option.getValue()).thenReturn("s-1");

        when(resolver.resolveTriggerPropertyOptions(
            eq(triggerDefinitionService), eq(triggerDefinitionFacade), any(), eq("googleSheets"), eq(1),
            eq("newRow"), eq("sheet"), any(), any(), any(), anyInt()))
                .thenReturn(new PropertyOptionsResolver.OptionsLookupResult.Success(List.of(option), false));
        when(resolver.buildSuccessEnvelope(
            eq("googleSheets"), eq("triggerName"), eq("newRow"), eq("sheet"), any(), eq(false)))
                .thenReturn(Map.of("options", List.of()));

        String result = toolCallback.call(
            "{\"kind\": \"TRIGGER\", \"componentName\": \"googleSheets\", \"operationName\": \"newRow\", "
                + "\"propertyName\": \"sheet\"}",
            toolContext());

        assertThat(result).contains("options");

        verify(resolver, never()).resolveActionPropertyOptions(
            any(), any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), anyInt());
    }

    private ToolContext toolContext() {
        return new ToolContext(
            new AgentToolInvocationContext(1L, null, 0L, "thread-1", null).toToolContext());
    }
}
