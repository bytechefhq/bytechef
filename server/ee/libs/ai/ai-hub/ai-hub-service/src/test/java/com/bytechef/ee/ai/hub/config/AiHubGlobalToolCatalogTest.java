/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.exception.AGUIException;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import com.bytechef.ee.ai.hub.toolsearch.AiHubGlobalToolCatalog;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskDispatcherTools;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubGlobalToolCatalogTest {

    private final ComponentTools componentTools = new ComponentTools(
        mock(ActionDefinitionFacade.class), mock(ComponentDefinitionFacade.class),
        mock(TriggerDefinitionFacade.class), null);
    private final TaskDispatcherTools taskDispatcherTools = new TaskDispatcherTools(null);
    private final TaskTools taskTools = new TaskTools(componentTools, taskDispatcherTools);
    private final AiHubConfiguration configuration = new AiHubConfiguration(
        new ByteArrayResource(new byte[0]), new ByteArrayResource(new byte[0]));

    @Test
    void testCatalogHoldsTheComponentTaskAndTaskDispatcherTools() {
        assertThat(toolNames(catalogToolCallbacks()))
            .contains(
                "listComponents", "searchComponents", "listActions", "getActionDefinition", "getTask", "listTasks",
                "validateTask", "listTaskDispatchers", "getTaskDispatcherDefinition");
    }

    @Test
    void testCatalogLeavesOutThePinnedComponentDefinitionTools() {
        assertThat(toolNames(catalogToolCallbacks())).doesNotContainAnyElementsOf(
            AiHubConfiguration.COMPONENT_DEFINITION_TOOL_NAMES);
    }

    @Test
    void testCatalogHasNoDuplicateToolNames() {
        assertThat(toolNames(catalogToolCallbacks())).doesNotHaveDuplicates();
    }

    @Test
    void testCatalogHasNoToolThatIsPinned() throws AGUIException {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        List<String> catalogToolNames = toolNames(catalogToolCallbacks());

        assertThat(pinnedToolNames(fixtures.askAgent())).doesNotContainAnyElementsOf(catalogToolNames);
        assertThat(pinnedToolNames(fixtures.buildAgent())).doesNotContainAnyElementsOf(catalogToolNames);
    }

    @Test
    void testCatalogIsEmptyWithoutTheToolBeans() {
        assertThat(
            AiHubConfiguration.catalogToolCallbacks(provider(null), provider(null), provider(null))).isEmpty();
    }

    @Test
    void testEachModeCatalogUsesItsOwnSession() {
        AiHubGlobalToolCatalog askCatalog = configuration.aiHubAskGlobalToolCatalog(
            provider(componentTools), provider(taskTools), provider(taskDispatcherTools));
        AiHubGlobalToolCatalog buildCatalog = configuration.aiHubBuildGlobalToolCatalog(
            provider(componentTools), provider(taskTools), provider(taskDispatcherTools));

        assertThat(askCatalog.sessionId()).isEqualTo(ToolSearchCatalogFeeder.GLOBAL_ASK_SESSION_ID);
        assertThat(buildCatalog.sessionId()).isEqualTo(ToolSearchCatalogFeeder.GLOBAL_BUILD_SESSION_ID);
        assertThat(toolNames(askCatalog.toolCallbacks())).isEqualTo(toolNames(buildCatalog.toolCallbacks()));
    }

    private List<ToolCallback> catalogToolCallbacks() {
        return AiHubConfiguration.catalogToolCallbacks(
            provider(componentTools), provider(taskTools), provider(taskDispatcherTools));
    }

    private static List<String> pinnedToolNames(AiHubSpringAIAgent agent) {
        return toolNames(agent.pinnedToolCallbacks());
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(value);

        return objectProvider;
    }
}
