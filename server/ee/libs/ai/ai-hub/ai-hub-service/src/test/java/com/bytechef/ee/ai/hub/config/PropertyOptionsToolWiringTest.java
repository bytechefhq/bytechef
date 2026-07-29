/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.copilot.tool.PropertyOptionsResolver;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolFacade;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins that the shared state-visibility registration helper adds the property-options lookup tools. Both the ASK and
 * BUILD agent bean methods delegate to this single helper, so presence here guarantees presence in both catalogs.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PropertyOptionsToolWiringTest {

    @Test
    void testRegistersLookupOptionsTools() throws Exception {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        PropertyOptionsResolver resolver = new PropertyOptionsResolver(
            new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

        Method method = AiHubConfiguration.class.getDeclaredMethod(
            "registerToolAttachStateVisibilityToolCallbacks", List.class, AiHubTaskService.class,
            AiHubTaskToolFacade.class, ComponentDefinitionService.class, ConnectionDefinitionService.class,
            WorkspaceConnectionFacade.class, ActionDefinitionService.class, ActionDefinitionFacade.class,
            TriggerDefinitionService.class, TriggerDefinitionFacade.class, PropertyOptionsResolver.class,
            AiHubToolAttachMetrics.class, JsonMapper.class);

        method.setAccessible(true);

        method.invoke(
            null, toolCallbacks, mock(AiHubTaskService.class), mock(AiHubTaskToolFacade.class),
            mock(ComponentDefinitionService.class), mock(ConnectionDefinitionService.class),
            mock(WorkspaceConnectionFacade.class), mock(ActionDefinitionService.class),
            mock(ActionDefinitionFacade.class), mock(TriggerDefinitionService.class),
            mock(TriggerDefinitionFacade.class), resolver, mock(AiHubToolAttachMetrics.class), new JsonMapper());

        List<String> toolNames = toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(toolNames).contains("listConnectionsForComponent", "lookupPropertyOptions", "selectPropertyOption");
        // The action/trigger twins were consolidated into the kind-keyed pair above — the twins must NOT
        // reappear on the pinned list.
        assertThat(toolNames).doesNotContain(
            "lookupActionPropertyOptions", "lookupTriggerPropertyOptions", "selectTriggerPropertyOption");
    }
}
