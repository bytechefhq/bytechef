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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.PollFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.util.TokenRefreshHelper;
import com.bytechef.platform.constant.PlatformType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class TriggerDefinitionServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ComponentDefinitionRegistry componentDefinitionRegistry;

    @Mock
    private ContextFactory contextFactory;

    @Mock
    private TokenRefreshHelper tokenRefreshHelper;

    @Mock
    private TriggerContext triggerContext;

    @BeforeEach
    void setUpMocks() {
        when(contextFactory.createTriggerContext(
            Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(triggerContext);

        Mockito.lenient()
            .when(tokenRefreshHelper.executeSingleConnectionFunction(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.any(TriggerContext.class),
                Mockito.any(), Mockito.any(), Mockito.any()))
            .thenAnswer(invocation -> {
                Object componentConnection = invocation.getArgument(2);
                TriggerContext triggerContext1 = invocation.getArgument(3);
                BiFunction<Object, TriggerContext, Object> performFn = invocation.getArgument(5);
                return performFn.apply(componentConnection, triggerContext1);
            });
    }

    @Test
    public void testExecutePollingTrigger() {
        TriggerDefinition mockTriggerDefinition = mock(TriggerDefinition.class);

        when(mockTriggerDefinition.getType()).thenReturn(TriggerType.POLLING);

        PollFunction mockPollFunction =
            (inputParameters, connectionParameters, closureParameters, context) -> new PollOutput(
                List.of("Test Record"), Map.of("lastPolledAt", System.currentTimeMillis()), false);

        when(mockTriggerDefinition.getPoll()).thenReturn(Optional.of(mockPollFunction));
        when(mockTriggerDefinition.getBatch()).thenReturn(Optional.of(false));

        when(componentDefinitionRegistry.getTriggerDefinition("testComponent", 1, "testTrigger"))
            .thenReturn(mockTriggerDefinition);

        TriggerDefinitionServiceImpl triggerDefinitionService = new TriggerDefinitionServiceImpl(
            componentDefinitionRegistry, contextFactory, eventPublisher, tokenRefreshHelper);

        TriggerOutput output = triggerDefinitionService.executeTrigger(
            "testComponent", 1, "testTrigger", null, null, Collections.emptyMap(), null, null, null, null,
            PlatformType.AUTOMATION, false);

        assertNotNull(output, "TriggerOutput should not be null");
        assertNotNull(output.value(), "Output should not be null");
        assertInstanceOf(List.class, output.value(), "Output should be a List");

        List<?> records = (List<?>) output.value();

        assertTrue(records.contains("Test Record"), "Output should contain the test record");

        assertNotNull(output.state(), "Closure parameters should not be null");
        assertTrue(((Map<?, ?>) output.state()).containsKey(
            "lastPolledAt"), "Closure parameters should contain lastPolledAt");
    }

    @Test
    public void testExecutePollingTriggerWithProviderException() {
        TriggerDefinition mockTriggerDefinition = mock(TriggerDefinition.class);

        when(mockTriggerDefinition.getType()).thenReturn(TriggerType.POLLING);

        ProviderException providerException = new ProviderException("Test provider exception");

        PollFunction mockPollFunction = (inputParameters, connectionParameters, closureParameters, context) -> {
            throw providerException;
        };

        when(mockTriggerDefinition.getPoll()).thenReturn(Optional.of(mockPollFunction));

        when(componentDefinitionRegistry.getTriggerDefinition("testComponent", 1, "testTrigger"))
            .thenReturn(mockTriggerDefinition);

        TriggerDefinitionServiceImpl triggerDefinitionService = new TriggerDefinitionServiceImpl(
            componentDefinitionRegistry, contextFactory, eventPublisher, tokenRefreshHelper);

        ProviderException thrownException = assertThrows(ProviderException.class, () -> {
            triggerDefinitionService.executeTrigger(
                "testComponent", 1, "testTrigger", null, null, Collections.emptyMap(), null, null, null, null,
                PlatformType.AUTOMATION, false);
        });

        assertSame(
            providerException, thrownException,
            "The thrown exception should be the same instance as the original ProviderException");
    }

    @Disabled
    @Test
    public void testGetTriggerDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetTriggerDefinitions() {
        // TODO
    }
}
