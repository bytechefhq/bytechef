/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowRunDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowRunStatus;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver.CallableWorkflow;
import com.bytechef.error.ExecutionError;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class, properties = "bytechef.public-url=https://bc.example")
@Import(PostgreSQLContainerConfiguration.class)
class AppWorkflowExecutionFacadeIntTest {

    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(30);
    private static final long DEPLOYMENT_ID = 11L;
    private static final String INPUT_SCHEMA = "{\"type\": \"object\", \"required\": [\"orderId\"]}";
    private static final long JOB_ID = 99L;
    private static final String LEGACY_WORKFLOW_KEY = "legacy_flow";
    private static final String LEGACY_WORKFLOW_UUID = "5d6c1e2f-7a8b-4c9d-9e0f-1a2b3c4d5e6f";
    private static final String OUTPUT_SCHEMA = "{\"type\": \"object\"}";
    private static final String TENANT_ID = "public";
    private static final String WORKFLOW_ID = "wf-2";
    private static final String WORKFLOW_KEY = "approve_refund";
    private static final String WORKFLOW_UUID = "0b9f0b0e-5f1e-4d0a-8c52-2c3a0a8d8b11";

    @MockitoSpyBean
    private AppCallableWorkflowResolver appCallableWorkflowResolver;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppService appService;

    @Autowired
    private AppWorkflowExecutionFacade appWorkflowExecutionFacade;

    @Autowired
    private AppWorkflowService appWorkflowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobCompletionAwaiter jobCompletionAwaiter;

    @Autowired
    private JobService jobService;

    @Autowired
    private PrincipalJobFacade principalJobFacade;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private ToolExecutionRecorder toolExecutionRecorder;

    @Autowired
    private WorkflowService workflowService;

    private volatile long appIdA;
    private volatile long appIdB;

    @BeforeEach
    void beforeEach() {
        appIdA = createApp("refunds-a");
        appIdB = createApp("refunds-b");

        stubCallableWorkflow();

        Job job = new Job(JOB_ID);

        job.setStatus(Job.Status.STARTED);

        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(JOB_ID);
        when(jobCompletionAwaiter.await(anyLong(), any(Duration.class)))
            .thenReturn(CompletableFuture.failedFuture(new TimeoutException()));
        when(jobService.getJob(JOB_ID)).thenReturn(job);
        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(job));

        appWorkflowService.bind(appIdA, WORKFLOW_KEY, DEPLOYMENT_ID, WORKFLOW_UUID);

        authenticateAs(appIdA);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        appRepository.deleteAll();

        Mockito.reset(
            jobCompletionAwaiter, jobService, principalJobFacade, projectDeploymentService,
            projectDeploymentWorkflowService, projectService, projectWorkflowService, taskExecutionService,
            taskFileStorage, toolExecutionRecorder, workflowService);
    }

    @Test
    void testRunIsPollableByTheAppThatStartedIt() {
        AppWorkflowRunDTO started = appWorkflowExecutionFacade.executeWorkflow(
            WORKFLOW_KEY, Map.of("orderId", "o-1"), 1);

        assertThat(started.runId()).isNotBlank();
        assertThat(started).isEqualTo(
            new AppWorkflowRunDTO(started.runId(), WORKFLOW_KEY, AppWorkflowRunStatus.RUNNING, null, null, null));
        assertThat(started.isFinished()).isFalse();

        AppWorkflowRunDTO polled = appWorkflowExecutionFacade.getWorkflowRun(started.runId());

        assertThat(polled.runId()).isEqualTo(started.runId());
        assertThat(polled.workflowKey()).isEqualTo(WORKFLOW_KEY);
        assertThat(polled.status()).isEqualTo(AppWorkflowRunStatus.RUNNING);
    }

    @Test
    void testAnotherAppCannotPollTheRun() {
        AppWorkflowRunDTO started = appWorkflowExecutionFacade.executeWorkflow(
            WORKFLOW_KEY, Map.of("orderId", "o-1"), 1);

        assertThat(countRows("app_workflow_run")).isEqualTo(1L);

        authenticateAs(appIdB);

        assertThatThrownBy(() -> appWorkflowExecutionFacade.getWorkflowRun(started.runId()))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND.getErrorKey());

        authenticateAs(appIdA);

        assertThat(appWorkflowExecutionFacade.getWorkflowRun(started.runId())
            .status()).isEqualTo(AppWorkflowRunStatus.RUNNING);
    }

    @Test
    void testDeletingTheAppRemovesItsRunsAndBindings() {
        appWorkflowExecutionFacade.executeWorkflow(WORKFLOW_KEY, Map.of("orderId", "o-1"), 1);

        assertThat(countRows("app_workflow")).isEqualTo(1L);
        assertThat(countRows("app_workflow_run")).isEqualTo(1L);

        appRepository.deleteById(appIdA);

        assertThat(countRows("app_workflow")).isZero();
        assertThat(countRows("app_workflow_run")).isZero();
    }

    @Test
    void testRunThatFailsAfterTheJobStartedIsRecordedAsError() {
        IllegalStateException brokerDown = new IllegalStateException("broker down");

        when(jobCompletionAwaiter.await(anyLong(), any(Duration.class)))
            .thenReturn(CompletableFuture.failedFuture(brokerDown));

        assertThatThrownBy(() -> appWorkflowExecutionFacade.executeWorkflow(
            WORKFLOW_KEY, Map.of("orderId", "o-1"), 1))
                .isInstanceOf(CompletionException.class)
                .hasCause(brokerDown);

        ToolExecutionEvent event = captureRecordedEvent();

        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.ERROR);
        assertThat(event.appId()).isEqualTo(appIdA);
        assertThat(event.jobId()).isEqualTo(JOB_ID);
        assertThat(event.errorType()).isEqualTo(CompletionException.class.getName());
        assertThat(event.errorMessage()).startsWith("Run %s: ".formatted(fetchRunId()));
    }

    @Test
    void testCompletedRunReturnsTheResultAndCreatesTheJobLikeMcp() {
        AtomicReference<Object> principalInsideCreateJob = new AtomicReference<>();
        AtomicReference<JobParametersDTO> jobParametersDTOReference = new AtomicReference<>();
        FileEntry outputs = mock(FileEntry.class);

        when(principalJobFacade.createJob(any(JobParametersDTO.class), anyLong(), any(PlatformType.class)))
            .thenAnswer(invocation -> {
                Authentication authentication = Objects.requireNonNull(
                    SecurityContextHolder.getContext()
                        .getAuthentication());

                principalInsideCreateJob.set(authentication.getPrincipal());
                jobParametersDTOReference.set(invocation.getArgument(0));

                return JOB_ID;
            });

        Job job = job(Job.Status.COMPLETED);

        job.setOutputs(outputs);

        doReturn(Map.of("approved", true)).when(taskFileStorage)
            .readJobOutputs(outputs);
        when(jobCompletionAwaiter.await(JOB_ID, DEFAULT_WAIT)).thenReturn(CompletableFuture.completedFuture(job));

        AppWorkflowRunDTO appWorkflowRunDTO = executeWorkflow(null);

        assertThat(appWorkflowRunDTO).isEqualTo(
            new AppWorkflowRunDTO(
                fetchRunId(), WORKFLOW_KEY, AppWorkflowRunStatus.COMPLETED, Map.of("approved", true), null, null));

        assertThat(principalInsideCreateJob.get()).isInstanceOf(AppPrincipal.class);

        JobParametersDTO jobParametersDTO = jobParametersDTOReference.get();

        assertThat(jobParametersDTO.getWorkflowId()).isEqualTo(WORKFLOW_ID);
        assertThat(jobParametersDTO.getInputs()).isEqualTo(
            Map.of(
                "region", "eu", "trigger_1", Map.of("orderId", "o-1"), JobInputConstants.TRIGGER_NAME_INPUT,
                "trigger_1"));

        verify(principalJobFacade).createJob(
            any(JobParametersDTO.class), eq(DEPLOYMENT_ID), eq(PlatformType.AUTOMATION));
    }

    @Test
    void testRunStillGoingAfterTheDefaultWaitIsRunning() {
        AppWorkflowRunDTO appWorkflowRunDTO = executeWorkflow(null);

        assertThat(appWorkflowRunDTO).isEqualTo(
            new AppWorkflowRunDTO(fetchRunId(), WORKFLOW_KEY, AppWorkflowRunStatus.RUNNING, null, null, null));
        assertThat(appWorkflowRunDTO.isFinished()).isFalse();

        verify(jobCompletionAwaiter).await(JOB_ID, DEFAULT_WAIT);
    }

    @Test
    void testRunWaitingOnAnApprovalReportsTheFormUrlAndIsRecordedAsApprovalRequired() {
        Job job = job(Job.Status.STOPPED);

        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1"));

        when(jobCompletionAwaiter.await(JOB_ID, DEFAULT_WAIT)).thenReturn(CompletableFuture.completedFuture(job));

        AppWorkflowRunDTO appWorkflowRunDTO = executeWorkflow(null);

        assertThat(appWorkflowRunDTO).isEqualTo(
            new AppWorkflowRunDTO(
                fetchRunId(), WORKFLOW_KEY, AppWorkflowRunStatus.APPROVAL_REQUIRED, null, null,
                "https://bc.example/resume/resume-1"));
        assertThat(appWorkflowRunDTO.isFinished()).isFalse();
        assertThat(captureRecordedEvent().outcome()).isEqualTo(ToolExecutionOutcome.APPROVAL_REQUIRED);
    }

    @Test
    void testStoppedRunWithoutAResumeIdIsCancelled() {
        when(jobCompletionAwaiter.await(JOB_ID, DEFAULT_WAIT))
            .thenReturn(CompletableFuture.completedFuture(job(Job.Status.STOPPED)));

        AppWorkflowRunDTO appWorkflowRunDTO = executeWorkflow(null);

        assertThat(appWorkflowRunDTO).isEqualTo(
            new AppWorkflowRunDTO(fetchRunId(), WORKFLOW_KEY, AppWorkflowRunStatus.CANCELLED, null, null, null));
        assertThat(appWorkflowRunDTO.isFinished()).isTrue();
    }

    @Test
    void testFailedRunIsDataNotAnExceptionAndIsRecordedAsError() {
        TaskExecution taskExecution = mock(TaskExecution.class);

        when(taskExecution.getStatus()).thenReturn(TaskExecution.Status.FAILED);
        when(taskExecution.getError()).thenReturn(new ExecutionError("Refund service is down", List.of()));
        when(taskExecutionService.fetchLastJobTaskExecution(JOB_ID)).thenReturn(Optional.of(taskExecution));
        when(jobCompletionAwaiter.await(JOB_ID, DEFAULT_WAIT))
            .thenReturn(CompletableFuture.completedFuture(job(Job.Status.FAILED)));

        AppWorkflowRunDTO appWorkflowRunDTO = executeWorkflow(null);

        assertThat(appWorkflowRunDTO).isEqualTo(
            new AppWorkflowRunDTO(
                fetchRunId(), WORKFLOW_KEY, AppWorkflowRunStatus.FAILED, null, "Refund service is down", null));
        assertThat(appWorkflowRunDTO.isFinished()).isTrue();

        ToolExecutionEvent event = captureRecordedEvent();

        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.ERROR);
        assertThat(event.errorMessage()).isEqualTo("Refund service is down");
    }

    @Test
    void testZeroWaitReadsTheJobWithoutAwaiting() {
        when(jobService.getJob(JOB_ID)).thenReturn(job(Job.Status.CREATED));

        AppWorkflowRunDTO appWorkflowRunDTO = executeWorkflow(0);

        assertThat(appWorkflowRunDTO).isEqualTo(
            new AppWorkflowRunDTO(fetchRunId(), WORKFLOW_KEY, AppWorkflowRunStatus.RUNNING, null, null, null));

        verifyNoInteractions(jobCompletionAwaiter);
    }

    @Test
    void testWaitOutOfRangeIsRejected() {
        assertConfigurationError(() -> executeWorkflow(-1), AppErrorType.APP_WORKFLOW_INPUT_INVALID);
        assertConfigurationError(() -> executeWorkflow(61), AppErrorType.APP_WORKFLOW_INPUT_INVALID);

        verifyNoInteractions(principalJobFacade);

        assertThat(countRows("app_workflow_run")).isZero();
    }

    @Test
    void testInvalidInputIsRejectedBeforeAnyJob() {
        assertThatThrownBy(() -> appWorkflowExecutionFacade.executeWorkflow(WORKFLOW_KEY, Map.of("amount", 3), null))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("orderId");

        assertConfigurationError(
            () -> appWorkflowExecutionFacade.executeWorkflow(WORKFLOW_KEY, Map.of("amount", 3), null),
            AppErrorType.APP_WORKFLOW_INPUT_INVALID);

        verifyNoInteractions(principalJobFacade);

        assertThat(countRows("app_workflow_run")).isZero();
    }

    @Test
    void testUnparseableInputSchemaIsUnavailable() {
        doReturn(
            Optional.of(
                new CallableWorkflow(
                    WORKFLOW_ID, "trigger_1", "Approve refund", null, "{not json", null, Map.of())))
                        .when(appCallableWorkflowResolver)
                        .resolve(1L, Environment.PRODUCTION, DEPLOYMENT_ID, WORKFLOW_UUID);

        assertConfigurationError(() -> executeWorkflow(null), AppErrorType.APP_WORKFLOW_UNAVAILABLE);
        assertThatThrownBy(() -> executeWorkflow(null)).hasCauseInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(principalJobFacade);
    }

    @Test
    void testUnknownWorkflowKeyIsNotFound() {
        assertConfigurationError(
            () -> appWorkflowExecutionFacade.executeWorkflow("unknown", Map.of("orderId", "o-1"), null),
            AppErrorType.APP_WORKFLOW_NOT_FOUND);

        verifyNoInteractions(principalJobFacade);
    }

    @Test
    void testUnresolvableBindingIsUnavailable() {
        when(projectDeploymentService.fetchProjectDeployment(DEPLOYMENT_ID)).thenReturn(Optional.empty());

        assertConfigurationError(() -> executeWorkflow(null), AppErrorType.APP_WORKFLOW_UNAVAILABLE);

        verifyNoInteractions(principalJobFacade);
    }

    @Test
    void testDisabledAppIsRefused() {
        appService.updateStatus(appIdA, AppStatus.DISABLED);

        assertConfigurationError(() -> executeWorkflow(null), AppErrorType.APP_DISABLED);
        assertConfigurationError(
            () -> appWorkflowExecutionFacade.getWorkflowRun("run-1"), AppErrorType.APP_DISABLED);
        assertConfigurationError(appWorkflowExecutionFacade::getWorkflows, AppErrorType.APP_DISABLED);

        verifyNoInteractions(principalJobFacade);
    }

    @Test
    void testExecutionIsRecordedWithTheJobAndOutcome() {
        when(jobCompletionAwaiter.await(JOB_ID, DEFAULT_WAIT))
            .thenReturn(CompletableFuture.completedFuture(job(Job.Status.COMPLETED)));

        executeWorkflow(null);

        ToolExecutionEvent event = captureRecordedEvent();

        assertThat(event.surface()).isEqualTo(ToolExecutionSurface.AUTOMATION_APP_WORKFLOW);
        assertThat(event.kind()).isEqualTo(ToolExecutionKind.WORKFLOW);
        assertThat(event.toolName()).isEqualTo(WORKFLOW_KEY);
        assertThat(event.operationName()).isEqualTo(WORKFLOW_KEY);
        assertThat(event.type()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(event.appId()).isEqualTo(appIdA);
        assertThat(event.workspaceId()).isEqualTo(1L);
        assertThat(event.environment()).isEqualTo(Environment.PRODUCTION.ordinal());
        assertThat(event.jobId()).isEqualTo(JOB_ID);
        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.SUCCESS);
    }

    @Test
    void testGetRunMapsTheCurrentJob() {
        AppWorkflowRunDTO started = executeWorkflow(1);

        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.of(job(Job.Status.COMPLETED)));

        AppWorkflowRunDTO appWorkflowRunDTO = appWorkflowExecutionFacade.getWorkflowRun(started.runId());

        assertThat(appWorkflowRunDTO).isEqualTo(
            new AppWorkflowRunDTO(started.runId(), WORKFLOW_KEY, AppWorkflowRunStatus.COMPLETED, null, null, null));
        assertThat(appWorkflowRunDTO.isFinished()).isTrue();
    }

    @Test
    void testGetRunOfAPurgedJobIsNotFound() {
        AppWorkflowRunDTO started = executeWorkflow(1);

        when(jobService.fetchJob(JOB_ID)).thenReturn(Optional.empty());

        assertConfigurationError(
            () -> appWorkflowExecutionFacade.getWorkflowRun(started.runId()),
            AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND);
    }

    @Test
    void testGetWorkflowsListsAvailabilityAndSchemas() {
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, LEGACY_WORKFLOW_UUID))
            .thenReturn(Optional.of(WORKFLOW_ID));

        appWorkflowService.bind(appIdA, LEGACY_WORKFLOW_KEY, DEPLOYMENT_ID, LEGACY_WORKFLOW_UUID);

        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, LEGACY_WORKFLOW_UUID))
            .thenReturn(Optional.empty());
        when(workflowService.fetchWorkflow(WORKFLOW_ID)).thenReturn(Optional.of(workflow(INPUT_SCHEMA, null)));

        List<AppWorkflowDTO> appWorkflowDTOs = appWorkflowExecutionFacade.getWorkflows();

        assertThat(appWorkflowDTOs).hasSize(2);

        AppWorkflowDTO first = appWorkflowDTOs.get(0);

        assertThat(first.workflowKey()).isEqualTo(WORKFLOW_KEY);
        assertThat(first.name()).isEqualTo("Approve refund");
        assertThat(first.description()).isEqualTo("Asks a manager");
        assertThat(first.inputSchema()).containsEntry("type", "object");
        assertThat(first.outputSchema()).isNull();
        assertThat(first.available()).isTrue();

        assertThat(appWorkflowDTOs.get(1))
            .isEqualTo(new AppWorkflowDTO(LEGACY_WORKFLOW_KEY, LEGACY_WORKFLOW_KEY, null, null, null, false));
    }

    @Test
    void testGetWorkflowsMarksAnUnreadableInputSchemaUnavailable() {
        when(workflowService.fetchWorkflow(WORKFLOW_ID)).thenReturn(Optional.of(workflow("true", OUTPUT_SCHEMA)));

        assertThat(appWorkflowExecutionFacade.getWorkflows()).containsExactly(
            new AppWorkflowDTO(WORKFLOW_KEY, "Approve refund", "Asks a manager", null, null, false));
    }

    private static void assertConfigurationError(ThrowingCallable callable, AppErrorType errorType) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(errorType.getErrorKey());
    }

    private void authenticateAs(long appId) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new TestingAuthenticationToken(
                    new AppPrincipal(TENANT_ID, appId, 1L, Environment.PRODUCTION), null));
    }

    private ToolExecutionEvent captureRecordedEvent() {
        ArgumentCaptor<ToolExecutionEvent> eventCaptor = ArgumentCaptor.forClass(ToolExecutionEvent.class);

        verify(toolExecutionRecorder).record(eventCaptor.capture());

        return eventCaptor.getValue();
    }

    private long countRows(String tableName) {
        return JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    }

    private long createApp(String name) {
        return Objects.requireNonNull(
            TenantContext.callWithTenantId(TENANT_ID, () -> appService.create(newApp(name)))
                .app()
                .getId());
    }

    private AppWorkflowRunDTO executeWorkflow(@Nullable Integer waitSeconds) {
        return appWorkflowExecutionFacade.executeWorkflow(WORKFLOW_KEY, Map.of("orderId", "o-1"), waitSeconds);
    }

    private String fetchRunId() {
        return jdbcTemplate.queryForObject("SELECT run_id FROM app_workflow_run", String.class);
    }

    private void stubCallableWorkflow() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(DEPLOYMENT_ID);
        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setProjectId(3L);

        Project project = new Project();

        project.setId(3L);
        project.setWorkspaceId(1L);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setInputs(Map.of("region", "eu"));
        projectDeploymentWorkflow.setProjectDeploymentId(DEPLOYMENT_ID);
        projectDeploymentWorkflow.setWorkflowId(WORKFLOW_ID);

        when(projectDeploymentService.fetchProjectDeployment(DEPLOYMENT_ID)).thenReturn(Optional.of(projectDeployment));
        when(projectService.fetchProject(3L)).thenReturn(Optional.of(project));
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID))
            .thenReturn(Optional.of(WORKFLOW_ID));
        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(DEPLOYMENT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(projectDeploymentWorkflow));
        when(workflowService.fetchWorkflow(WORKFLOW_ID)).thenReturn(Optional.of(workflow(INPUT_SCHEMA, OUTPUT_SCHEMA)));
    }

    private static Job job(Job.Status status) {
        Job job = new Job(JOB_ID);

        job.setStatus(status);

        return job;
    }

    private static App newApp(String name) {
        App app = new App();

        app.setName(name);
        app.setWorkspaceId(1L);
        app.setEnvironment(Environment.PRODUCTION);
        app.setOwnerUserId(1L);
        app.setHostingTarget(AppHostingTarget.VERCEL);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        return app;
    }

    private static Workflow workflow(String inputSchema, @Nullable String outputSchema) {
        String responseParameters = outputSchema == null
            ? "{}" : "{\"outputSchema\": \"%s\"}".formatted(escape(outputSchema));

        return new Workflow(
            WORKFLOW_ID,
            """
                {"label": "Approve refund", "description": "Asks a manager", \
                 "triggers": [{"name": "trigger_1", "type": "workflow/v1/newWorkflowCall", \
                               "parameters": {"inputSchema": "%s"}}], \
                 "tasks": [{"name": "workflow_1", "type": "workflow/v1/responseToWorkflowCall", \
                            "parameters": %s}]}\
                """.formatted(escape(inputSchema), responseParameters),
            Workflow.Format.JSON);
    }

    private static String escape(String json) {
        return json.replace("\"", "\\\"");
    }
}
