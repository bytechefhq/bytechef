/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.provider.VariableScopeProvider;
import com.bytechef.ee.platform.variable.service.VariableService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @version ee
 */
class WorkflowVariablesResolverTest {

    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private VariableScopeProvider automationProvider;
    private VariableService variableService;
    private WorkflowVariablesResolverImpl resolver;

    @BeforeEach
    void beforeEach() {
        jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
        automationProvider = mock(VariableScopeProvider.class);
        variableService = mock(VariableService.class);

        when(automationProvider.getType()).thenReturn(PlatformType.AUTOMATION);

        resolver = new WorkflowVariablesResolverImpl(
            jobPrincipalAccessorRegistry, variableService, List.of(automationProvider));
    }

    @Test
    void testResolveForJobPrincipalUsesProviderScopeAndAccessorEnvironment() {
        JobPrincipalAccessor accessor = mock(JobPrincipalAccessor.class);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION)).thenReturn(accessor);
        when(accessor.getEnvironmentId(42L)).thenReturn(2L);
        when(automationProvider.getVariableScope(42L)).thenReturn(Optional.of(VariableScope.workspace(7L)));
        when(variableService.getVariableMap(VariableScope.workspace(7L), 2L)).thenReturn(Map.of("A", "1"));

        assertThat(resolver.resolveForJobPrincipal(42L, PlatformType.AUTOMATION)).containsExactly(Map.entry("A", "1"));
    }

    @Test
    void testResolveForJobPrincipalIsEmptyWithoutProviderForType() {
        assertThat(resolver.resolveForJobPrincipal(42L, PlatformType.EMBEDDED)).isEmpty();
    }

    @Test
    void testResolveForJobPrincipalIsEmptyWhenProviderScopeIsEmpty() {
        when(automationProvider.getVariableScope(42L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveForJobPrincipal(42L, PlatformType.AUTOMATION)).isEmpty();
    }

    @Test
    void testResolveForWorkflowAsksEveryProvider() {
        when(automationProvider.getVariableScopeByWorkflowId("wf"))
            .thenReturn(Optional.of(VariableScope.workspace(7L)));
        when(variableService.getVariableMap(VariableScope.workspace(7L), 0L)).thenReturn(Map.of("B", "2"));

        assertThat(resolver.resolveForWorkflow("wf", 0L)).containsExactly(Map.entry("B", "2"));
    }

    @Test
    void testResolveForWorkflowIsEmptyWhenNoProviderClaimsTheWorkflow() {
        when(automationProvider.getVariableScopeByWorkflowId("wf")).thenReturn(Optional.empty());

        assertThat(resolver.resolveForWorkflow("wf", 0L)).isEmpty();
    }

    @Test
    void testResolverFailsOpenOnException() {
        when(automationProvider.getVariableScopeByWorkflowId("wf")).thenThrow(new UnsupportedOperationException());

        assertThat(resolver.resolveForWorkflow("wf", 0L)).isEmpty();
    }

    @Test
    void testFailureIsLoggedOnceAcrossMultipleFailingCalls() {
        when(automationProvider.getVariableScopeByWorkflowId("wf")).thenThrow(new UnsupportedOperationException());

        ListAppender<ILoggingEvent> appender = attachAppenderToResolverLogger();

        try {
            assertThat(resolver.resolveForWorkflow("wf", 0L)).isEmpty();
            assertThat(resolver.resolveForWorkflow("wf", 0L)).isEmpty();
            assertThat(resolver.resolveForWorkflow("wf", 0L)).isEmpty();

            assertThat(appender.list)
                .as("only the first failure logs at WARN; later failures in the same JVM stay at DEBUG")
                .filteredOn(event -> event.getLevel() == Level.WARN)
                .hasSize(1);
        } finally {
            detachAppenderFromResolverLogger(appender);
        }
    }

    private static ListAppender<ILoggingEvent> attachAppenderToResolverLogger() {
        Logger logger = (Logger) LoggerFactory.getLogger(WorkflowVariablesResolverImpl.class);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();

        appender.start();
        logger.addAppender(appender);

        return appender;
    }

    private static void detachAppenderFromResolverLogger(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(WorkflowVariablesResolverImpl.class);

        logger.detachAppender(appender);
        appender.stop();
    }
}
