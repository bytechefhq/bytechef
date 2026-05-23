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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.PollFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.constant.PlatformType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
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
    private TriggerContext triggerContext;

    @BeforeEach
    void setUpMocks() {
        when(contextFactory.createTriggerContext(
            Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
                .thenReturn(triggerContext);
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
            componentDefinitionRegistry, contextFactory, eventPublisher);

        TriggerOutput output = triggerDefinitionService.executeTrigger(
            "testComponent", 1, "testTrigger", null, null, Collections.emptyMap(), null, null, null, null,
            PlatformType.AUTOMATION, false);

        assertNotNull(output, "TriggerOutput should not be null");
        assertNotNull(output.value(), "Output should not be null");
        assertInstanceOf(List.class, output.value(), "Output should be a List");

        List<?> records = (List<?>) output.value();

        assertTrue(records.contains("Test Record"), "Output should contain the test record");

        assertNotNull(output.state(), "Closure parameters should not be null");
        assertTrue(
            ((Map<?, ?>) output.state()).containsKey("lastPolledAt"), "Closure parameters should contain lastPolledAt");
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
            componentDefinitionRegistry, contextFactory, eventPublisher);

        ProviderException thrownException = assertThrows(ProviderException.class, () -> {
            triggerDefinitionService.executeTrigger(
                "testComponent", 1, "testTrigger", null, null, Collections.emptyMap(), null, null, null, null,
                PlatformType.AUTOMATION, false);
        });

        assertSame(
            providerException, thrownException,
            "The thrown exception should be the same instance as the original ProviderException");
    }

    @Test
    public void testExecutePollingTriggerStopsAfterMaxIterations() {
        TriggerDefinition mockTriggerDefinition = mock(TriggerDefinition.class);

        when(mockTriggerDefinition.getType()).thenReturn(TriggerType.POLLING);

        AtomicInteger pollCount = new AtomicInteger();

        // A component that always asks to be polled again (pollImmediately = true), advancing its cursor every
        // call. It stops only after a large, finite number of pages purely so this test cannot hang if the
        // production poll loop is unbounded.
        PollFunction mockPollFunction =
            (inputParameters, connectionParameters, closureParameters, context) -> {
                int page = pollCount.incrementAndGet();

                return new PollOutput(List.of("record-" + page), Map.of("cursor", page), page < 5000);
            };

        when(mockTriggerDefinition.getPoll()).thenReturn(Optional.of(mockPollFunction));
        when(mockTriggerDefinition.getBatch()).thenReturn(Optional.of(false));

        when(componentDefinitionRegistry.getTriggerDefinition("testComponent", 1, "testTrigger"))
            .thenReturn(mockTriggerDefinition);

        TriggerDefinitionServiceImpl triggerDefinitionService = new TriggerDefinitionServiceImpl(
            componentDefinitionRegistry, contextFactory, eventPublisher);

        TriggerOutput output = triggerDefinitionService.executeTrigger(
            "testComponent", 1, "testTrigger", null, null, Collections.emptyMap(), null, null, null, null,
            PlatformType.AUTOMATION, false);

        List<?> records = (List<?>) output.value();

        // The production cap is MAX_POLLING_TRIGGER_ITERATIONS=100 in-loop iterations, plus the initial poll that
        // runs before the loop — so a single execution can call pollFunction at most 101 times. The bound is set to
        // exactly that so any off-by-one regression in the iteration cap fails this test.
        assertTrue(
            pollCount.get() <= 101,
            "Polling must stop at MAX_POLLING_TRIGGER_ITERATIONS + 1 calls, but pollFunction was called " +
                pollCount.get() + " times");
        assertTrue(
            records.size() <= 101,
            "Accumulated records must be bounded by the iteration cap, but " + records.size() +
                " records were collected");
    }

    @Test
    public void testExecutePollingTriggerStopsAfterMaxRecords() {
        TriggerDefinition mockTriggerDefinition = mock(TriggerDefinition.class);

        when(mockTriggerDefinition.getType()).thenReturn(TriggerType.POLLING);

        AtomicInteger pollCount = new AtomicInteger();

        // Each poll returns a large batch and asks to be polled again. It stops only after a finite number of
        // pages so the test terminates even if the production poll loop fails to bound accumulation.
        PollFunction mockPollFunction =
            (inputParameters, connectionParameters, closureParameters, context) -> {
                int page = pollCount.incrementAndGet();

                List<Object> batch = new ArrayList<>();

                for (int i = 0; i < 500; i++) {
                    batch.add("record-" + page + "-" + i);
                }

                return new PollOutput(batch, Map.of("cursor", page), page < 100);
            };

        when(mockTriggerDefinition.getPoll()).thenReturn(Optional.of(mockPollFunction));
        when(mockTriggerDefinition.getBatch()).thenReturn(Optional.of(false));

        when(componentDefinitionRegistry.getTriggerDefinition("testComponent", 1, "testTrigger"))
            .thenReturn(mockTriggerDefinition);

        TriggerDefinitionServiceImpl triggerDefinitionService = new TriggerDefinitionServiceImpl(
            componentDefinitionRegistry, contextFactory, eventPublisher);

        TriggerOutput output = triggerDefinitionService.executeTrigger(
            "testComponent", 1, "testTrigger", null, null, Collections.emptyMap(), null, null, null, null,
            PlatformType.AUTOMATION, false);

        List<?> records = (List<?>) output.value();

        // The production cap is MAX_POLLING_TRIGGER_RECORDS=10_000. The pollFunction returns 500 records per page,
        // so a correctly-capped run accumulates exactly 10_000; this bound catches any record-cap overshoot.
        assertTrue(
            records.size() <= 10_000,
            "Accumulated records must be bounded by MAX_POLLING_TRIGGER_RECORDS, but " + records.size() +
                " records were collected");
    }

    @Test
    public void testExecutePollingTriggerLogsWarningWhenCapTrips() {
        TriggerDefinition mockTriggerDefinition = mock(TriggerDefinition.class);

        when(mockTriggerDefinition.getType()).thenReturn(TriggerType.POLLING);

        // A component that never finishes paginating: it always asks to be polled again. The production poll
        // loop must stop it at a safety cap and log a warning that names the runaway trigger.
        PollFunction mockPollFunction =
            (inputParameters, connectionParameters, closureParameters, context) -> new PollOutput(
                List.of("record"), Map.of("cursor", System.nanoTime()), true);

        when(mockTriggerDefinition.getPoll()).thenReturn(Optional.of(mockPollFunction));
        when(mockTriggerDefinition.getBatch()).thenReturn(Optional.of(false));

        when(componentDefinitionRegistry.getTriggerDefinition("testComponent", 1, "testTrigger"))
            .thenReturn(mockTriggerDefinition);

        TriggerDefinitionServiceImpl triggerDefinitionService = new TriggerDefinitionServiceImpl(
            componentDefinitionRegistry, contextFactory, eventPublisher);

        ListAppender<ILoggingEvent> logAppender = attachLogAppender();

        try {
            triggerDefinitionService.executeTrigger(
                "testComponent", 1, "testTrigger", null, null, Collections.emptyMap(), null, null, null, null,
                PlatformType.AUTOMATION, false);
        } finally {
            detachLogAppender(logAppender);
        }

        boolean warningLogged = logAppender.list.stream()
            .anyMatch(event -> {
                String formattedMessage = event.getFormattedMessage();

                return event.getLevel() == Level.WARN && formattedMessage.contains("testComponent")
                    && formattedMessage.contains("testTrigger");
            });

        assertTrue(
            warningLogged, "A WARN log naming the runaway trigger should be emitted when a polling safety cap trips");
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

    private static ListAppender<ILoggingEvent> attachLogAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(TriggerDefinitionServiceImpl.class);

        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

        logAppender.start();
        logger.addAppender(logAppender);

        return logAppender;
    }

    private static void detachLogAppender(ListAppender<ILoggingEvent> logAppender) {
        Logger logger = (Logger) LoggerFactory.getLogger(TriggerDefinitionServiceImpl.class);

        logger.detachAppender(logAppender);
    }
}
