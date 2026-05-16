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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.ActionDefinition.WebSocketPerformFunction;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.MultipleConnectionsWebSocketPerformFunction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the two bidirectional WebSocket perform contracts are dispatched instead of falling through to the
 * blind {@code MultipleConnectionsSseStreamResponsePerformFunction} cast (which threw {@link ClassCastException} and
 * blocked every realtime/voice action from executing).
 *
 * @author Ivica Cardic
 */
class ActionDefinitionServiceWebSocketPerformTest {

    private final ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
    private final ContextFactory contextFactory = mock(ContextFactory.class);
    private final ActionDefinitionServiceImpl actionDefinitionService = new ActionDefinitionServiceImpl(
        componentDefinitionRegistry, contextFactory, List.of());

    @Test
    void testExecutePerformDispatchesWebSocketPerformFunction() {
        WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);

        WebSocketPerformFunction performFunction =
            (inputParameters, connectionParameters, context) -> webSocketHandler;

        Object result = executePerform(performFunction);

        assertThat(result).isSameAs(webSocketHandler);
    }

    @Test
    void testExecutePerformDispatchesMultipleConnectionsWebSocketPerformFunction() {
        WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);

        MultipleConnectionsWebSocketPerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> webSocketHandler;

        Object result = executePerform(performFunction);

        assertThat(result).isSameAs(webSocketHandler);
    }

    private Object executePerform(Object performFunction) {
        com.bytechef.component.definition.ActionDefinition actionDefinition =
            mock(com.bytechef.component.definition.ActionDefinition.class);

        when(componentDefinitionRegistry.getActionDefinition("example", 1, "perform")).thenReturn(actionDefinition);
        when(actionDefinition.getResumePerform()).thenReturn(Optional.empty());
        doReturn(Optional.of(performFunction)).when(actionDefinition)
            .getPerform();

        when(
            contextFactory.createActionContext(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(mock(ActionContext.class));

        return actionDefinitionService.executePerform(
            "example", 1, "perform", null, null, null, null, "workflow1", Map.of(), Map.of(), Map.of(), 0L, false, null,
            null, null, null);
    }
}
