/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppWorkflow;
import com.bytechef.ee.automation.app.domain.AppWorkflowRun;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppWorkflowRepository;
import com.bytechef.ee.automation.app.repository.AppWorkflowRunRepository;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver.CallableWorkflow;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppWorkflowServiceTest {

    private static final String UUID = "0b0f3a3e-0000-4000-8000-000000000001";

    @Mock
    private AppCallableWorkflowResolver appCallableWorkflowResolver;

    @Mock
    private AppService appService;

    @Mock
    private AppWorkflowRepository appWorkflowRepository;

    @Mock
    private AppWorkflowRunRepository appWorkflowRunRepository;

    private App app;
    private AppWorkflowServiceImpl appWorkflowService;
    private CallableWorkflow callableWorkflow;

    @BeforeEach
    void beforeEach() {
        app = new App();

        app.setId(1L);
        app.setWorkspaceId(7L);
        app.setEnvironment(Environment.PRODUCTION);

        callableWorkflow = new CallableWorkflow("wf-2", "trigger_1", "Approve refund", null, null, null, Map.of());

        appWorkflowService = new AppWorkflowServiceImpl(
            appCallableWorkflowResolver, appService, appWorkflowRepository, appWorkflowRunRepository);
    }

    @Test
    void testBindCreatesTheBinding() {
        when(appService.getApp(1L)).thenReturn(app);
        when(appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, 11L, UUID))
            .thenReturn(Optional.of(callableWorkflow));
        when(appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(1L)).thenReturn(List.of());
        when(appWorkflowRepository.findByAppIdAndWorkflowKey(1L, "approve_refund")).thenReturn(Optional.empty());
        when(appWorkflowRepository.save(any(AppWorkflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppWorkflow binding = appWorkflowService.bind(1L, "approve_refund", 11L, UUID);

        assertThat(binding.getAppId()).isEqualTo(1L);
        assertThat(binding.getProjectDeploymentId()).isEqualTo(11L);
        assertThat(binding.getWorkflowKey()).isEqualTo("approve_refund");
        assertThat(binding.getWorkflowUuid()).isEqualTo(UUID);
    }

    @Test
    void testBindReplacesAnExistingBindingInPlace() {
        AppWorkflow existing = appWorkflow(5L, "approve_refund", 3L, "11111111-1111-4111-8111-111111111111");

        when(appService.getApp(1L)).thenReturn(app);
        when(appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, 11L, UUID))
            .thenReturn(Optional.of(callableWorkflow));
        when(appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(1L)).thenReturn(List.of(existing));
        when(appWorkflowRepository.findByAppIdAndWorkflowKey(1L, "approve_refund")).thenReturn(Optional.of(existing));
        when(appWorkflowRepository.save(any(AppWorkflow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppWorkflow binding = appWorkflowService.bind(1L, "approve_refund", 11L, UUID);

        assertThat(binding.getId()).isEqualTo(5L);
        assertThat(binding.getProjectDeploymentId()).isEqualTo(11L);
        assertThat(binding.getWorkflowUuid()).isEqualTo(UUID);
    }

    @Test
    void testBindRejectsAnInvalidKey() {
        lenient().when(appService.getApp(1L))
            .thenReturn(app);
        lenient().when(appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, 11L, UUID))
            .thenReturn(Optional.of(callableWorkflow));
        lenient().when(appWorkflowRepository.save(any(AppWorkflow.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertConfigurationError(
            () -> appWorkflowService.bind(1L, "Approve", 11L, UUID), AppErrorType.APP_BINDING_KEY_INVALID);

        verify(appWorkflowRepository, never()).save(any());
    }

    @Test
    void testBindRejectsAKeyWhoseFlatFormCollides() {
        AppWorkflow existing = appWorkflow(5L, "approve-refund", 3L, UUID);

        lenient().when(appService.getApp(1L))
            .thenReturn(app);
        lenient().when(appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, 11L, UUID))
            .thenReturn(Optional.of(callableWorkflow));
        when(appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(1L)).thenReturn(List.of(existing));
        lenient().when(appWorkflowRepository.save(any(AppWorkflow.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertConfigurationError(
            () -> appWorkflowService.bind(1L, "approve_refund", 11L, UUID),
            AppErrorType.APP_BINDING_KEY_ALREADY_EXISTS);

        verify(appWorkflowRepository, never()).save(any());
    }

    @Test
    void testBindRejectsAMalformedWorkflowUuid() {
        lenient().when(appService.getApp(1L))
            .thenReturn(app);
        when(appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(1L)).thenReturn(List.of());
        lenient().when(appWorkflowRepository.save(any(AppWorkflow.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertConfigurationError(
            () -> appWorkflowService.bind(1L, "approve_refund", 11L, "nope"), AppErrorType.APP_WORKFLOW_NOT_BINDABLE);

        verifyNoInteractions(appCallableWorkflowResolver);
        verify(appWorkflowRepository, never()).save(any());
    }

    @Test
    void testBindRejectsANullWorkflowUuid() {
        lenient().when(appService.getApp(1L))
            .thenReturn(app);
        when(appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(1L)).thenReturn(List.of());
        lenient().when(appWorkflowRepository.save(any(AppWorkflow.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertConfigurationError(
            () -> appWorkflowService.bind(1L, "approve_refund", 11L, null), AppErrorType.APP_WORKFLOW_NOT_BINDABLE);

        verifyNoInteractions(appCallableWorkflowResolver);
        verify(appWorkflowRepository, never()).save(any());
    }

    @Test
    void testBindRejectsAWorkflowThatIsNotCallable() {
        when(appService.getApp(1L)).thenReturn(app);
        when(appCallableWorkflowResolver.resolve(7L, Environment.PRODUCTION, 11L, UUID)).thenReturn(Optional.empty());
        when(appWorkflowRepository.findAllByAppIdOrderByWorkflowKey(1L)).thenReturn(List.of());
        lenient().when(appWorkflowRepository.save(any(AppWorkflow.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        assertConfigurationError(
            () -> appWorkflowService.bind(1L, "approve_refund", 11L, UUID), AppErrorType.APP_WORKFLOW_NOT_BINDABLE);

        verify(appWorkflowRepository, never()).save(any());
    }

    @Test
    void testUnbindReportsWhetherABindingWasDeleted() {
        AppWorkflow existing = appWorkflow(5L, "approve_refund", 3L, UUID);

        when(appWorkflowRepository.findByAppIdAndWorkflowKey(1L, "approve_refund")).thenReturn(Optional.of(existing));
        when(appWorkflowRepository.findByAppIdAndWorkflowKey(1L, "missing")).thenReturn(Optional.empty());

        assertThat(appWorkflowService.unbind(1L, "approve_refund")).isTrue();

        verify(appWorkflowRepository).delete(existing);

        assertThat(appWorkflowService.unbind(1L, "missing")).isFalse();

        verify(appWorkflowRepository, times(1)).delete(any(AppWorkflow.class));
    }

    @Test
    void testCreateRunAssignsAFreshRunId() {
        when(appWorkflowRunRepository.save(any(AppWorkflowRun.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AppWorkflowRun first = appWorkflowService.createRun(1L, "approve_refund", 99L);
        AppWorkflowRun second = appWorkflowService.createRun(1L, "approve_refund", 99L);

        assertThat(first.getRunId()).hasSize(36);
        assertThat(java.util.UUID.fromString(first.getRunId())).isNotNull();
        assertThat(first.getAppId()).isEqualTo(1L);
        assertThat(first.getWorkflowKey()).isEqualTo("approve_refund");
        assertThat(first.getJobId()).isEqualTo(99L);
        assertThat(first.getCreatedDate()).isNotNull();
        assertThat(second.getRunId()).isNotEqualTo(first.getRunId());
    }

    @Test
    void testFetchRunIsScopedToTheApp() {
        AppWorkflowRun appWorkflowRun = new AppWorkflowRun();

        when(appWorkflowRunRepository.findByAppIdAndRunId(1L, "r")).thenReturn(Optional.of(appWorkflowRun));

        assertThat(appWorkflowService.fetchRun(1L, "r")).containsSame(appWorkflowRun);
        assertThat(appWorkflowService.fetchRun(2L, "r")).isEmpty();
    }

    @Test
    void testDeleteRunsCreatedBeforeDelegates() {
        Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        when(appWorkflowRunRepository.deleteByCreatedDateBefore(instant)).thenReturn(4);

        assertThat(appWorkflowService.deleteRunsCreatedBefore(instant)).isEqualTo(4);
    }

    private static AppWorkflow appWorkflow(long id, String workflowKey, long projectDeploymentId, String workflowUuid) {
        AppWorkflow appWorkflow = new AppWorkflow();

        appWorkflow.setAppId(1L);
        appWorkflow.setId(id);
        appWorkflow.setProjectDeploymentId(projectDeploymentId);
        appWorkflow.setWorkflowKey(workflowKey);
        appWorkflow.setWorkflowUuid(workflowUuid);

        return appWorkflow;
    }

    private static void assertConfigurationError(ThrowingCallable callable, AppErrorType errorType) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(errorType.getErrorKey());
    }
}
