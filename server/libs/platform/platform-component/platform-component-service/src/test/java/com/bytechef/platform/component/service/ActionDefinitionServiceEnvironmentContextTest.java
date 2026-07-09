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
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ActionDefinitionServiceEnvironmentContextTest {

    @BeforeEach
    @AfterEach
    void clearEnvironmentContext() {
        EnvironmentContext.clear();
    }

    @Test
    void testExecutePerformBindsEnvironmentContextFromEnvironmentId() {
        ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
        ContextFactory contextFactory = mock(ContextFactory.class);

        ActionDefinitionServiceImpl service = new ActionDefinitionServiceImpl(
            componentDefinitionRegistry, contextFactory);

        AtomicReference<Environment> capturedEnvironment = new AtomicReference<>();

        MultipleConnectionsPerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> {
                capturedEnvironment.set(EnvironmentContext.getCurrentEnvironment());

                return "ok";
            };

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

        service.executePerform(
            "example", 1, "perform", null, null, null, null, "workflow1", Map.of(), Map.of(), Map.of(), 0L, false,
            null, null, null, null);

        assertThat(capturedEnvironment.get()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(EnvironmentContext.fetchCurrentEnvironment()).isNull();
    }
}
