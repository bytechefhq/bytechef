/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.domain.AppWorkflow;
import com.bytechef.ee.automation.app.domain.AppWorkflowRun;
import com.bytechef.ee.automation.app.dto.AppWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowRunDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowRunStatus;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver.CallableWorkflow;
import com.bytechef.ee.automation.app.workflow.AppWorkflowSchemas;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.JobExecutionErrors;
import com.bytechef.platform.workflow.execution.WorkflowCallResults;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runs the workflows bound to the calling app. The facade is deliberately not transactional: a run is waited for up to
 * the configured limit, and the job is created as the app request, not as a system actor.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class AppWorkflowExecutionFacadeImpl implements AppWorkflowExecutionFacade {

    private static final Logger log = LoggerFactory.getLogger(AppWorkflowExecutionFacadeImpl.class);

    private final AppCallableWorkflowResolver appCallableWorkflowResolver;
    private final ApplicationProperties applicationProperties;
    private final ObjectProvider<ApprovalTokens> approvalTokensObjectProvider;
    private final AppService appService;
    private final AppWorkflowService appWorkflowService;
    private final JobCompletionAwaiter jobCompletionAwaiter;
    private final JobService jobService;
    private final PrincipalJobFacade principalJobFacade;
    private final @Nullable String publicUrl;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final ToolExecutionRecorder toolExecutionRecorder;

    @SuppressFBWarnings("EI")
    public AppWorkflowExecutionFacadeImpl(
        AppCallableWorkflowResolver appCallableWorkflowResolver, ApplicationProperties applicationProperties,
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider, AppService appService,
        AppWorkflowService appWorkflowService, JobCompletionAwaiter jobCompletionAwaiter, JobService jobService,
        PrincipalJobFacade principalJobFacade, @Value("${bytechef.public-url:#{null}}") @Nullable String publicUrl,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        ToolExecutionRecorder toolExecutionRecorder) {

        this.appCallableWorkflowResolver = appCallableWorkflowResolver;
        this.applicationProperties = applicationProperties;
        this.approvalTokensObjectProvider = approvalTokensObjectProvider;
        this.appService = appService;
        this.appWorkflowService = appWorkflowService;
        this.jobCompletionAwaiter = jobCompletionAwaiter;
        this.jobService = jobService;
        this.principalJobFacade = principalJobFacade;
        this.publicUrl = publicUrl;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.toolExecutionRecorder = toolExecutionRecorder;
    }

    @Override
    public AppWorkflowRunDTO executeWorkflow(
        String workflowKey, Map<String, Object> input, @Nullable Integer waitSeconds) {

        AppPrincipal principal = AppPrincipal.getCurrent();
        App app = getEnabledApp(principal);

        AppWorkflow binding = appWorkflowService.fetchBinding(Objects.requireNonNull(app.getId()), workflowKey)
            .orElseThrow(() -> new ConfigurationException(
                "No workflow is bound to the app under '%s'".formatted(workflowKey),
                AppErrorType.APP_WORKFLOW_NOT_FOUND));
        CallableWorkflow callableWorkflow = appCallableWorkflowResolver.resolve(
            Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"), app.getEnvironment(),
            binding.getProjectDeploymentId(), binding.getWorkflowUuid())
            .orElseThrow(() -> unavailable(workflowKey));

        Duration wait = resolveWait(waitSeconds);

        checkInput(workflowKey, binding.getProjectDeploymentId(), callableWorkflow, input);

        Map<String, Object> inputs = new HashMap<>(callableWorkflow.deploymentInputs());

        inputs.put(callableWorkflow.triggerName(), input);
        inputs.put(JobInputConstants.TRIGGER_NAME_INPUT, callableWorkflow.triggerName());

        long start = System.nanoTime();

        long jobId = principalJobFacade.createJob(
            new JobParametersDTO(callableWorkflow.workflowId(), inputs), binding.getProjectDeploymentId(),
            PlatformType.AUTOMATION);

        String runId = null;
        AppWorkflowRunDTO appWorkflowRunDTO;

        try {
            AppWorkflowRun appWorkflowRun = appWorkflowService.createRun(app.getId(), workflowKey, jobId);

            runId = appWorkflowRun.getRunId();

            appWorkflowRunDTO = toRunDTO(appWorkflowRun, awaitJob(jobId, wait));
        } catch (RuntimeException runtimeException) {
            recordFailure(
                principal, app, workflowKey, jobId, runId, runtimeException,
                Duration.ofNanos(System.nanoTime() - start));

            throw runtimeException;
        }

        record(principal, app, workflowKey, jobId, appWorkflowRunDTO, Duration.ofNanos(System.nanoTime() - start));

        return appWorkflowRunDTO;
    }

    @Override
    public AppWorkflowRunDTO getWorkflowRun(String runId) {
        App app = getEnabledApp(AppPrincipal.getCurrent());

        AppWorkflowRun appWorkflowRun = appWorkflowService.fetchRun(Objects.requireNonNull(app.getId()), runId)
            .orElseThrow(() -> runNotFound(runId));
        Job job = jobService.fetchJob(appWorkflowRun.getJobId())
            .orElseThrow(() -> runNotFound(runId));

        return toRunDTO(appWorkflowRun, job);
    }

    @Override
    public List<AppWorkflowDTO> getWorkflows() {
        App app = getEnabledApp(AppPrincipal.getCurrent());

        return appWorkflowService.getBindings(Objects.requireNonNull(app.getId()))
            .stream()
            .map(binding -> appCallableWorkflowResolver.resolve(
                Objects.requireNonNull(app.getWorkspaceId(), "workspaceId"), app.getEnvironment(),
                binding.getProjectDeploymentId(), binding.getWorkflowUuid())
                .map(callableWorkflow -> toWorkflowDTO(binding, callableWorkflow))
                .orElseGet(() -> new AppWorkflowDTO(binding.getWorkflowKey(), binding.getWorkflowKey(), null, null,
                    null, false)))
            .toList();
    }

    private Job awaitJob(long jobId, Duration wait) {
        if (wait.isZero()) {
            return jobService.getJob(jobId);
        }

        try {
            return jobCompletionAwaiter.await(jobId, wait)
                .join();
        } catch (CompletionException completionException) {
            if (completionException.getCause() instanceof TimeoutException) {
                return jobService.getJob(jobId);
            }

            throw completionException;
        }
    }

    private void checkInput(
        String workflowKey, Long projectDeploymentId, CallableWorkflow callableWorkflow, Map<String, Object> input) {

        List<String> errors;

        try {
            errors = AppWorkflowSchemas.validate(callableWorkflow.inputSchema(), input);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn(
                "The input schema of workflow '{}' of deployment {} cannot be read", workflowKey,
                projectDeploymentId, illegalArgumentException);

            throw new ConfigurationException(
                "The input schema of workflow '%s' cannot be read".formatted(workflowKey), illegalArgumentException,
                AppErrorType.APP_WORKFLOW_UNAVAILABLE);
        }

        if (!errors.isEmpty()) {
            throw new ConfigurationException(
                "Invalid input for workflow '%s': %s".formatted(workflowKey, String.join("; ", errors)),
                AppErrorType.APP_WORKFLOW_INPUT_INVALID);
        }
    }

    private App getEnabledApp(AppPrincipal principal) {
        App app = appService.getApp(principal.appId());

        if (app.getStatus() != AppStatus.ENABLED) {
            throw new ConfigurationException("App is disabled", AppErrorType.APP_DISABLED);
        }

        return app;
    }

    private void record(
        AppPrincipal principal, App app, String workflowKey, long jobId, AppWorkflowRunDTO appWorkflowRunDTO,
        Duration duration) {

        ToolExecutionEvent.Builder builder = eventBuilder(principal, app, workflowKey, jobId, duration);

        switch (appWorkflowRunDTO.status()) {
            case FAILED -> builder.outcome(ToolExecutionOutcome.ERROR)
                .errorMessage(appWorkflowRunDTO.errorMessage());
            case APPROVAL_REQUIRED -> builder.outcome(ToolExecutionOutcome.APPROVAL_REQUIRED);
            default -> builder.outcome(ToolExecutionOutcome.SUCCESS);
        }

        toolExecutionRecorder.record(builder.build());
    }

    private void recordFailure(
        AppPrincipal principal, App app, String workflowKey, long jobId, @Nullable String runId,
        RuntimeException runtimeException, Duration duration) {

        String errorMessage = Objects.requireNonNullElse(
            runtimeException.getMessage(), runtimeException.getClass()
                .getSimpleName());

        if (runId != null) {
            errorMessage = "Run %s: %s".formatted(runId, errorMessage);
        }

        ToolExecutionEvent.Builder builder = eventBuilder(principal, app, workflowKey, jobId, duration)
            .outcome(ToolExecutionOutcome.ERROR)
            .errorType(runtimeException.getClass()
                .getName())
            .errorMessage(errorMessage);

        try {
            toolExecutionRecorder.record(builder.build());
        } catch (RuntimeException recordException) {
            runtimeException.addSuppressed(recordException);
        }
    }

    private ToolExecutionEvent.Builder eventBuilder(
        AppPrincipal principal, App app, String workflowKey, long jobId, Duration duration) {

        return ToolExecutionEvent
            .builder(PlatformType.AUTOMATION, ToolExecutionSurface.AUTOMATION_APP_WORKFLOW, ToolExecutionKind.WORKFLOW,
                workflowKey)
            .appId(app.getId())
            .workspaceId(app.getWorkspaceId())
            .environment(app.getEnvironmentId())
            .jobId(jobId)
            .operationName(workflowKey)
            .durationMs(duration.toMillis());
    }

    private Duration resolveWait(@Nullable Integer waitSeconds) {
        ApplicationProperties.App.WorkflowRun workflow = applicationProperties.getApp()
            .getWorkflow();

        if (waitSeconds == null) {
            return workflow.getDefaultWait();
        }

        Duration maxWait = workflow.getMaxWait();

        if (waitSeconds < 0 || waitSeconds > maxWait.toSeconds()) {
            throw new ConfigurationException(
                "waitSeconds must be between 0 and %s".formatted(maxWait.toSeconds()),
                AppErrorType.APP_WORKFLOW_INPUT_INVALID);
        }

        return Duration.ofSeconds(waitSeconds);
    }

    private AppWorkflowRunDTO toRunDTO(AppWorkflowRun appWorkflowRun, Job job) {
        String runId = appWorkflowRun.getRunId();
        String workflowKey = appWorkflowRun.getWorkflowKey();

        return switch (job.getStatus()) {
            case CREATED, STARTED -> new AppWorkflowRunDTO(
                runId, workflowKey, AppWorkflowRunStatus.RUNNING, null, null, null);
            case COMPLETED -> new AppWorkflowRunDTO(
                runId, workflowKey, AppWorkflowRunStatus.COMPLETED,
                WorkflowCallResults.readResult(job, taskExecutionService, taskFileStorage), null, null);
            case FAILED, PAYMENT_REQUIRED -> new AppWorkflowRunDTO(
                runId, workflowKey, AppWorkflowRunStatus.FAILED, null, getErrorMessage(job), null);
            case STOPPED -> WorkflowCallResults.isPendingApproval(job)
                ? new AppWorkflowRunDTO(
                    runId, workflowKey, AppWorkflowRunStatus.APPROVAL_REQUIRED, null, null,
                    WorkflowCallResults.fetchApprovalFormUrl(
                        job, publicUrl, approvalTokensObjectProvider.getIfAvailable())
                        .orElse(null))
                : new AppWorkflowRunDTO(runId, workflowKey, AppWorkflowRunStatus.CANCELLED, null, null, null);
            case CANCELLED -> new AppWorkflowRunDTO(
                runId, workflowKey, AppWorkflowRunStatus.CANCELLED, null, null, null);
        };
    }

    private String getErrorMessage(Job job) {
        try {
            JobExecutionErrors.checkForError(job, taskExecutionService);
        } catch (ExecutionException executionException) {
            return executionException.getMessage();
        }

        return "The workflow run failed";
    }

    private static AppWorkflowDTO toWorkflowDTO(AppWorkflow binding, CallableWorkflow callableWorkflow) {
        Map<String, Object> inputSchema;

        try {
            inputSchema = AppWorkflowSchemas.parse(callableWorkflow.inputSchema());
        } catch (IllegalArgumentException illegalArgumentException) {
            return new AppWorkflowDTO(
                binding.getWorkflowKey(), callableWorkflow.label(), callableWorkflow.description(), null, null, false);
        }

        return new AppWorkflowDTO(
            binding.getWorkflowKey(), callableWorkflow.label(), callableWorkflow.description(), inputSchema,
            parseSchemaOrNull(callableWorkflow.outputSchema()), true);
    }

    private static @Nullable Map<String, Object> parseSchemaOrNull(@Nullable String schema) {
        try {
            return AppWorkflowSchemas.parse(schema);
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private static ConfigurationException runNotFound(String runId) {
        return new ConfigurationException(
            "No workflow run '%s' of this app".formatted(runId), AppErrorType.APP_WORKFLOW_RUN_NOT_FOUND);
    }

    private static ConfigurationException unavailable(String workflowKey) {
        return new ConfigurationException(
            ("Workflow '%s' is unavailable: its deployment or workflow is gone or disabled, or the workflow is no " +
                "longer triggered by Call workflow").formatted(workflowKey),
            AppErrorType.APP_WORKFLOW_UNAVAILABLE);
    }
}
