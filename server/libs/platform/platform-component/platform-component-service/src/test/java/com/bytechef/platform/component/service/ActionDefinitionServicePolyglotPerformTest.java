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

package com.bytechef.platform.component.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsStreamPerformFunction;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pins what a code workflow (or script) task reaches through the polyglot seam: a single-connection action, a
 * multi-connection action handed the caller's whole wiring, and — the reason the extensions argument exists at all — a
 * cluster-element action such as the AI Agent's chat, whose perform reads its model out of the extensions map.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ActionDefinitionServicePolyglotPerformTest {

    private final ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
    private final ContextFactory contextFactory = mock(ContextFactory.class);
    private final ActionDefinitionServiceImpl actionDefinitionService = new ActionDefinitionServiceImpl(
        componentDefinitionRegistry, contextFactory, List.of());

    @Test
    void testExecutePerformForPolyglotForwardsExtensionsToMultipleConnectionsPerform() {
        MultipleConnectionsPerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> extensions.getMap("clusterElements");

        Map<String, ?> clusterElements = Map.of("model", Map.of("type", "openAi/v1/model"));

        Object result = executePerformForPolyglot(
            performFunction, Map.of(), Map.of("clusterElements", clusterElements));

        assertThat(result).isEqualTo(clusterElements);
    }

    @Test
    void testExecutePerformForPolyglotHandsTheCallerWiringToMultipleConnectionsPerform() {
        MultipleConnectionsPerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> componentConnections;

        ComponentConnection componentConnection = new ComponentConnection("openAi", 1, 1L, Map.of(), null);

        Object result = executePerformForPolyglot(
            performFunction, Map.of("openai-prod", componentConnection), Map.of());

        assertThat(result).isEqualTo(Map.of("openai-prod", componentConnection));
    }

    @Test
    void testExecutePerformForPolyglotDispatchesSingleConnectionPerform() {
        PerformFunction performFunction = (inputParameters, connectionParameters, context) -> "result";

        assertThat(executePerformForPolyglot(performFunction, Map.of(), Map.of())).isEqualTo("result");
    }

    @Test
    void testExecutePerformForPolyglotRejectsAStreamingPerformWithoutBlamingClusterElements() {
        MultipleConnectionsStreamPerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> null;

        assertThatThrownBy(() -> executePerformForPolyglot(performFunction, Map.of(), Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be invoked from code")
            .hasMessageNotContaining("cluster elements");
    }

    private Object executePerformForPolyglot(
        Object performFunction, Map<String, ? extends ComponentConnection> componentConnections,
        Map<String, ?> extensions) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            mock(com.bytechef.component.definition.ActionDefinition.class);

        when(componentDefinitionRegistry.getActionDefinition("example", 1, "perform")).thenReturn(actionDefinition);
        doReturn(Optional.of(performFunction)).when(actionDefinition)
            .getPerform();

        return actionDefinitionService.executePerformForPolyglot(
            "example", 1, "perform", Map.of(), null, componentConnections, extensions, null,
            mock(ActionContext.class));
    }
}
