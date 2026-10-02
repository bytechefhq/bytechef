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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition.BeforeSuspendConsumer;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import com.bytechef.component.definition.Parameters;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.LogEntryBufferAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsResumePerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsSseStreamResponsePerformFunction;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class ActionDefinitionServiceTest {

    private ActionContext actionContext;
    private com.bytechef.component.definition.ActionDefinition actionDefinition;
    private ActionDefinitionServiceImpl actionDefinitionService;
    private ContextFactory contextFactory;

    @BeforeEach
    void beforeEach() {
        ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);

        contextFactory = mock(ContextFactory.class);

        actionDefinitionService = new ActionDefinitionServiceImpl(componentDefinitionRegistry, contextFactory);

        actionContext = mock(ActionContext.class, withSettings().extraInterfaces(LogEntryBufferAware.class));
        actionDefinition = mock(com.bytechef.component.definition.ActionDefinition.class);

        when(componentDefinitionRegistry.getActionDefinition("example", 1, "perform")).thenReturn(actionDefinition);
        when(actionDefinition.getResumePerform()).thenReturn(Optional.empty());
        when(
            contextFactory.createActionContext(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(actionContext);
    }

    @Disabled
    @Test
    public void testGetComponentActionDefinition() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetComponentActionDefinitions() {
        // TODO
    }

    @Test
    void testExecutePerformFlushesBufferedLogEntriesAfterASuccessfulPerform() {
        MultipleConnectionsPerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> "ok";

        doReturn(Optional.of(performFunction)).when(actionDefinition)
            .getPerform();

        actionDefinitionService.executePerform(
            "example", 1, "perform", null, null, 1L, 10L, "workflow1", Map.of(), Map.of(), Map.of(), null, false,
            null, null, null, null);

        verify((LogEntryBufferAware) actionContext).flushLogEntries();
    }

    @Test
    void testExecutePerformFlushesBufferedLogEntriesWhenThePerformThrows() {
        MultipleConnectionsPerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> {
                throw new IllegalStateException("boom");
            };

        doReturn(Optional.of(performFunction)).when(actionDefinition)
            .getPerform();

        assertThrows(
            RuntimeException.class, () -> actionDefinitionService.executePerform(
                "example", 1, "perform", null, null, 1L, 10L, "workflow1", Map.of(), Map.of(), Map.of(), null,
                false, null, null, null, null));

        verify((LogEntryBufferAware) actionContext).flushLogEntries();
    }

    @Test
    void testExecutePerformDispatchesAMultipleConnectionsResumePerform() {
        Map<String, ComponentConnection> componentConnections = Map.of(
            "model_1", new ComponentConnection("example", 1, 1L, Map.of(), null));
        AtomicReference<Map<String, ComponentConnection>> receivedComponentConnections = new AtomicReference<>();
        AtomicReference<Parameters> receivedExtensions = new AtomicReference<>();

        MultipleConnectionsResumePerformFunction resumePerformFunction =
            (inputParameters, connections, extensions, continueParameters, data, context) -> {
                receivedComponentConnections.set(connections);
                receivedExtensions.set(extensions);

                return "resumed";
            };

        doReturn(Optional.of(resumePerformFunction)).when(actionDefinition)
            .getResumePerform();

        Object result = actionDefinitionService.executePerform(
            "example", 1, "perform", null, null, 1L, 10L, "workflow1", Map.of(), componentConnections,
            Map.of("clusterElements", Map.of()), null, false, null, Map.of("key", "value"), Map.of(), null);

        assertThat(result).isEqualTo("resumed");
        assertThat(receivedComponentConnections.get()).isSameAs(componentConnections);
        assertThat(receivedExtensions.get()
            .containsKey("clusterElements")).isTrue();
    }

    @Test
    void testExecutePerformReturnsASuspendRaisedWhileResuming() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);
        AtomicReference<Suspend> suspendReference = new AtomicReference<>();

        doAnswer(invocation -> {
            suspendReference.set(invocation.getArgument(0));

            return null;
        }).when(actionContextAware)
            .suspend(any());

        when(actionContextAware.getSuspend()).thenAnswer(invocation -> suspendReference.get());
        when(
            contextFactory.createActionContext(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(actionContextAware);

        MultipleConnectionsResumePerformFunction resumePerformFunction =
            (inputParameters, connections, extensions, continueParameters, data, context) -> {
                context.suspend(new Suspend(Map.of("pendingToolCallId", "call_2"), null));

                return null;
            };

        doReturn(Optional.of(resumePerformFunction)).when(actionDefinition)
            .getResumePerform();

        Object result = actionDefinitionService.executePerform(
            "example", 1, "perform", null, null, 1L, 10L, "workflow1", Map.of(), Map.of(), Map.of(), null, false,
            null, Map.of("pendingToolCallId", "call_1"), Map.of(), null);

        assertThat(result).isInstanceOf(Suspend.class);

        Map<String, ?> continueParameters = ((Suspend) result).continueParameters();

        assertThat(continueParameters.get("pendingToolCallId")).isEqualTo("call_2");
    }

    @Test
    void testExecutePerformRunsBeforeSuspendWhenAStreamedActionSuspends() throws Exception {
        ActionContextAware actionContextAware = mockSuspendingActionContext();
        BeforeSuspendConsumer beforeSuspendConsumer = mock(BeforeSuspendConsumer.class);
        Instant expiresAt = Instant.parse("2026-10-02T10:00:00Z");

        when(actionContextAware.getResumeUrl()).thenReturn("https://example.com/job/resume/1");
        when(actionDefinition.getBeforeSuspend()).thenReturn(Optional.of(beforeSuspendConsumer));

        MultipleConnectionsSseStreamResponsePerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> new SuspendAwareSseEmitterHandler(
                emitter -> {
                    context.suspend(new Suspend(Map.of("pendingToolCallId", "call_1"), expiresAt));

                    emitter.complete();
                },
                actionContextAware);

        doReturn(Optional.of(performFunction)).when(actionDefinition)
            .getPerform();

        Object result = actionDefinitionService.executePerform(
            "example", 1, "perform", null, null, 1L, 10L, "workflow1", Map.of(), Map.of(), Map.of(), null, false,
            null, null, null, null);

        assertThat(result).isInstanceOf(SuspendAwareSseEmitterHandler.class);

        verify(beforeSuspendConsumer, never()).apply(any(), any(), any(), any());

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler = (SuspendAwareSseEmitterHandler) result;

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        Suspend suspend = suspendAwareSseEmitterHandler.getSuspendOrThrow(1L);

        assertThat(suspend).isNotNull();

        suspendAwareSseEmitterHandler.getSuspendOrThrow(1L);

        verify(beforeSuspendConsumer).apply(
            eq("https://example.com/job/resume/1"),
            eq(expiresAt),
            argThat(
                parameters -> "call_1".equals(parameters.getString("pendingToolCallId"))),
            same(actionContextAware));
    }

    @Test
    void testExecutePerformFailsTheStreamedSuspendWhenBeforeSuspendThrows() throws Exception {
        ActionContextAware actionContextAware = mockSuspendingActionContext();
        BeforeSuspendConsumer beforeSuspendConsumer = mock(BeforeSuspendConsumer.class);

        doAnswer(invocation -> {
            throw new IllegalStateException("hook failed");
        }).when(beforeSuspendConsumer)
            .apply(any(), any(), any(), any());

        when(actionDefinition.getBeforeSuspend()).thenReturn(Optional.of(beforeSuspendConsumer));

        MultipleConnectionsSseStreamResponsePerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> new SuspendAwareSseEmitterHandler(
                emitter -> {
                    context.suspend(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

                    emitter.complete();
                },
                actionContextAware);

        doReturn(Optional.of(performFunction)).when(actionDefinition)
            .getPerform();

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler =
            (SuspendAwareSseEmitterHandler) actionDefinitionService.executePerform(
                "example", 1, "perform", null, null, 1L, 10L, "workflow1", Map.of(), Map.of(), Map.of(), null,
                false, null, null, null, null);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        assertThatThrownBy(() -> suspendAwareSseEmitterHandler.getSuspendOrThrow(1L))
            .isInstanceOf(ExecutionException.class)
            .hasRootCauseMessage("hook failed");
    }

    @Test
    void testExecutePerformLeavesAStreamedActionWithoutBeforeSuspendUnchanged() {
        ActionContextAware actionContextAware = mockSuspendingActionContext();

        when(actionDefinition.getBeforeSuspend()).thenReturn(Optional.empty());

        MultipleConnectionsSseStreamResponsePerformFunction performFunction =
            (inputParameters, componentConnections, extensions, context) -> new SuspendAwareSseEmitterHandler(
                emitter -> {
                    context.suspend(new Suspend(Map.of("pendingToolCallId", "call_1"), null));

                    emitter.complete();
                },
                actionContextAware);

        doReturn(Optional.of(performFunction)).when(actionDefinition)
            .getPerform();

        SuspendAwareSseEmitterHandler suspendAwareSseEmitterHandler =
            (SuspendAwareSseEmitterHandler) actionDefinitionService.executePerform(
                "example", 1, "perform", null, null, 1L, 10L, "workflow1", Map.of(), Map.of(), Map.of(), null,
                false, null, null, null, null);

        suspendAwareSseEmitterHandler.handle(mock(SseEmitter.class));

        Suspend suspend = suspendAwareSseEmitterHandler.getSuspendOrThrow(1L);

        assertThat(suspend).isNotNull();
        assertThat(suspend.continueParameters()
            .get("pendingToolCallId")).isEqualTo("call_1");
    }

    private ActionContextAware mockSuspendingActionContext() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);
        AtomicReference<Suspend> suspendReference = new AtomicReference<>();

        doAnswer(invocation -> {
            suspendReference.set(invocation.getArgument(0));

            return null;
        }).when(actionContextAware)
            .suspend(any());

        when(actionContextAware.getSuspend()).thenAnswer(invocation -> suspendReference.get());
        when(
            contextFactory.createActionContext(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(actionContextAware);

        return actionContextAware;
    }
}
