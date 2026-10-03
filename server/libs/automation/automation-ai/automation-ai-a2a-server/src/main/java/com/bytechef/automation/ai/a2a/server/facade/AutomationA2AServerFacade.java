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

package com.bytechef.automation.ai.a2a.server.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.ai.a2a.util.A2aWorkflowTriggerUtils;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor.A2ASkill;
import com.bytechef.platform.ai.a2a.A2AAgentExecutor;
import com.bytechef.platform.ai.a2a.A2AAgentRequest;
import com.bytechef.platform.ai.a2a.A2AAgentResult;
import com.bytechef.platform.ai.a2a.A2AAgentRun;
import com.bytechef.platform.ai.a2a.A2AInvalidParamsException;
import com.bytechef.platform.ai.a2a.A2ATaskReference;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.JobExecutionErrors;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.token.ApprovalFormUrls;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
public class AutomationA2AServerFacade implements A2AAgentExecutor {

    static final String A2A_CONTEXT_ID_METADATA_KEY = "a2aContextId";
    static final String A2A_TASK_NONCE_METADATA_KEY = "a2aTaskNonce";
    static final String MESSAGE_INPUT = "message";
    static final String RUN_IN_PROGRESS_MESSAGE =
        "The workflow run is still in progress. Poll tasks/get with this task id for the result.";

    private static final String AGENT_VERSION = "1.0.0";

    private static final Logger log = LoggerFactory.getLogger(AutomationA2AServerFacade.class);

    private final A2aProjectService a2aProjectService;
    private final A2aProjectWorkflowService a2aProjectWorkflowService;
    private final A2aServerService a2aServerService;
    private final ObjectProvider<ApprovalTokens> approvalTokensObjectProvider;
    private final JobCompletionAwaiter jobCompletionAwaiter;
    private final JobService jobService;
    private final PrincipalJobFacade principalJobFacade;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final @Nullable String publicUrl;
    private final Executor runCompletionExecutor;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AutomationA2AServerFacade(
        A2aProjectService a2aProjectService, A2aProjectWorkflowService a2aProjectWorkflowService,
        A2aServerService a2aServerService, ObjectProvider<ApprovalTokens> approvalTokensObjectProvider,
        JobCompletionAwaiter jobCompletionAwaiter, JobService jobService,
        PrincipalJobFacade principalJobFacade, PrincipalJobService principalJobService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, @Nullable String publicUrl,
        Executor runCompletionExecutor, TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        WorkflowService workflowService) {

        this.a2aProjectService = a2aProjectService;
        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
        this.a2aServerService = a2aServerService;
        this.approvalTokensObjectProvider = approvalTokensObjectProvider;
        this.jobCompletionAwaiter = jobCompletionAwaiter;
        this.jobService = jobService;
        this.principalJobFacade = principalJobFacade;
        this.principalJobService = principalJobService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.publicUrl = publicUrl;
        this.runCompletionExecutor = runCompletionExecutor;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    public A2AAgentDescriptor getAgentDescriptor(String secretKey, String url) {
        A2aServer a2aServer = a2aServerService.getA2aServer(secretKey);

        List<A2ASkill> skills = new ArrayList<>();

        for (ExposedWorkflow exposedWorkflow : getExposedWorkflows(a2aServer)) {
            skills.add(toSkill(exposedWorkflow));
        }

        return new A2AAgentDescriptor(a2aServer.getName(), a2aServer.getDescription(), url, AGENT_VERSION, skills);
    }

    @Override
    public A2AAgentRun start(A2AAgentRequest request) {
        A2aServer a2aServer = a2aServerService.getA2aServer(request.agentId());

        if (!a2aServer.isEnabled()) {
            return A2AAgentRun.of(A2AAgentResult.ofFailed("A2A server is disabled"));
        }

        List<ExposedWorkflow> exposedWorkflows = getExposedWorkflows(a2aServer);

        if (exposedWorkflows.isEmpty()) {
            return A2AAgentRun.of(
                A2AAgentResult.ofFailed(
                    "No enabled workflow with a New Workflow Call trigger is exposed by this A2A server"));
        }

        ExposedWorkflow exposedWorkflow = selectExposedWorkflow(exposedWorkflows, request.skillId());

        ProjectDeploymentWorkflow projectDeploymentWorkflow = exposedWorkflow.projectDeploymentWorkflow();

        Map<String, Object> inputs = new HashMap<>(projectDeploymentWorkflow.getInputs());

        inputs.put(exposedWorkflow.triggerName(), Map.of(MESSAGE_INPUT, request.text()));
        inputs.put(JobInputConstants.TRIGGER_NAME_INPUT, exposedWorkflow.triggerName());

        UUID nonce = UUID.randomUUID();
        String tenantId = TenantContext.getCurrentTenantId();

        Map<String, Object> metadata = Map.of(
            A2A_CONTEXT_ID_METADATA_KEY, request.contextId(), A2A_TASK_NONCE_METADATA_KEY, nonce.toString());

        long jobId = principalJobFacade.createJob(
            new JobParametersDTO(projectDeploymentWorkflow.getWorkflowId(), inputs, metadata),
            projectDeploymentWorkflow.getProjectDeploymentId(), PlatformType.AUTOMATION);

        A2aTaskId a2aTaskId = new A2aTaskId(jobId, nonce);

        A2ATaskReference taskReference = new A2ATaskReference(a2aTaskId.asString(), request.contextId());

        CompletableFuture<Job> jobFuture;

        try {
            jobFuture = jobCompletionAwaiter.await(jobId, JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT);
        } catch (RuntimeException runtimeException) {
            log.error("Failed to await A2A workflow run {}", jobId, runtimeException);

            return A2AAgentRun.started(
                taskReference, CompletableFuture.completedFuture(inProgressResult(taskReference)));
        }

        CompletableFuture<A2AAgentResult> result = jobFuture.handleAsync(
            (job, throwable) -> TenantContext.callWithTenantId(
                tenantId, () -> toRunResult(jobId, job, throwable, taskReference)),
            runCompletionExecutor);

        return A2AAgentRun.started(taskReference, result);
    }

    @Override
    public @Nullable A2AAgentResult pollTask(String agentId, String taskId) {
        A2aTaskId a2aTaskId = A2aTaskId.parse(taskId)
            .orElse(null);

        if (a2aTaskId == null) {
            return null;
        }

        Job job = jobService.fetchJob(a2aTaskId.jobId())
            .orElse(null);

        UUID nonce = a2aTaskId.nonce();

        if (job == null || !nonce.toString()
            .equals(job.getMetadata(A2A_TASK_NONCE_METADATA_KEY))) {

            return null;
        }

        if (!isA2aServerJob(agentId, a2aTaskId.jobId())) {
            return null;
        }

        Object contextId = job.getMetadata(A2A_CONTEXT_ID_METADATA_KEY);

        A2ATaskReference taskReference = new A2ATaskReference(
            taskId, contextId == null ? taskId : String.valueOf(contextId));

        return switch (job.getStatus()) {
            case CREATED, STARTED -> inProgressResult(taskReference);
            default -> toAgentResult(job, taskReference);
        };
    }

    private A2AAgentResult toRunResult(
        long jobId, @Nullable Job job, @Nullable Throwable throwable, A2ATaskReference taskReference) {

        if (throwable == null && job != null) {
            try {
                return toAgentResult(job, taskReference);
            } catch (RuntimeException runtimeException) {
                log.error("Failed to read the result of A2A workflow run {}", jobId, runtimeException);

                return inProgressResult(taskReference);
            }
        }

        Throwable cause = throwable instanceof CompletionException completionException &&
            completionException.getCause() != null ? completionException.getCause() : throwable;

        if (!(cause instanceof TimeoutException)) {
            log.error("Failed to await A2A workflow run {}", jobId, cause);
        }

        return inProgressResult(taskReference);
    }

    private static A2AAgentResult inProgressResult(A2ATaskReference taskReference) {
        return A2AAgentResult.ofWorking(RUN_IN_PROGRESS_MESSAGE, taskReference);
    }

    private A2AAgentResult toAgentResult(Job job, A2ATaskReference taskReference) {
        if (isPausedOnApproval(job)) {
            return A2AAgentResult.ofInputRequired(describePendingApproval(job), taskReference);
        }

        try {
            JobExecutionErrors.checkForError(job, taskExecutionService);
            JobExecutionErrors.checkCompleted(job);
        } catch (ExecutionException executionException) {
            log.warn("A2A workflow run {} did not complete", job.getId(), executionException);

            return A2AAgentResult.ofFailed("The workflow run " + job.getId() + " did not complete", taskReference);
        }

        return A2AAgentResult.ofCompleted(readOutputText(job), taskReference);
    }

    private static ExposedWorkflow selectExposedWorkflow(
        List<ExposedWorkflow> exposedWorkflows, @Nullable String skillId) {

        if (skillId == null) {
            if (exposedWorkflows.size() == 1) {
                return exposedWorkflows.getFirst();
            }

            throw new A2AInvalidParamsException(
                "This A2A server exposes several skills; set metadata.skillId to one of: " +
                    getSkillIds(exposedWorkflows));
        }

        return exposedWorkflows.stream()
            .filter(exposedWorkflow -> skillId.equals(getSkillId(exposedWorkflow)))
            .findFirst()
            .orElseThrow(() -> new A2AInvalidParamsException(
                "No skill with id " + skillId + " is exposed by this A2A server; valid skill ids: " +
                    getSkillIds(exposedWorkflows)));
    }

    private static String getSkillIds(List<ExposedWorkflow> exposedWorkflows) {
        return exposedWorkflows.stream()
            .map(AutomationA2AServerFacade::getSkillId)
            .collect(Collectors.joining(", "));
    }

    private static String getSkillId(ExposedWorkflow exposedWorkflow) {
        Workflow workflow = exposedWorkflow.workflow();

        return workflow.getId();
    }

    private List<ExposedWorkflow> getExposedWorkflows(A2aServer a2aServer) {
        List<ExposedWorkflow> exposedWorkflows = new ArrayList<>();

        for (A2aProject a2aProject : a2aProjectService.getA2aServerA2aProjects(a2aServer.getId())) {
            for (A2aProjectWorkflow a2aProjectWorkflow : a2aProjectWorkflowService
                .getA2aProjectA2aProjectWorkflows(a2aProject.getId())) {

                ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
                    .getProjectDeploymentWorkflow(a2aProjectWorkflow.getProjectDeploymentWorkflowId());

                if (!projectDeploymentWorkflow.isEnabled()) {
                    log.debug(
                        "A2A server {} skips workflow {}: its deployment workflow is disabled", a2aServer.getId(),
                        projectDeploymentWorkflow.getWorkflowId());

                    continue;
                }

                Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

                WorkflowTrigger workflowTrigger = A2aWorkflowTriggerUtils.fetchNewWorkflowCallTrigger(workflow)
                    .orElse(null);

                if (workflowTrigger == null) {
                    log.warn(
                        "A2A server {} skips workflow {}: it has no New Workflow Call trigger", a2aServer.getId(),
                        workflow.getId());

                    continue;
                }

                exposedWorkflows.add(
                    new ExposedWorkflow(
                        a2aProjectWorkflow, projectDeploymentWorkflow, workflow, workflowTrigger.getName()));
            }
        }

        return exposedWorkflows;
    }

    private boolean isA2aServerJob(String secretKey, long jobId) {
        Long projectDeploymentId = principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION)
            .orElse(null);

        if (projectDeploymentId == null) {
            return false;
        }

        A2aServer a2aServer = a2aServerService.getA2aServer(secretKey);

        return a2aProjectService.getA2aServerA2aProjects(a2aServer.getId())
            .stream()
            .anyMatch(a2aProject -> projectDeploymentId.equals(a2aProject.getProjectDeploymentId()));
    }

    private static boolean isPausedOnApproval(Job job) {
        return job.getStatus() == Job.Status.STOPPED && job.getMetadata(MetadataConstants.JOB_RESUME_ID) != null;
    }

    private String describePendingApproval(Job job) {
        Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        return ApprovalFormUrls
            .buildFormUrl(
                publicUrl, jobResumeId == null ? null : jobResumeId.toString(),
                approvalTokensObjectProvider.getIfAvailable())
            .map(formUrl -> "Approval required — the workflow run is paused waiting for a human decision. " +
                "Resolve it at: " + formUrl)
            .orElse("Approval required — the workflow run is paused waiting for a human decision.");
    }

    private String readOutputText(Job job) {
        if (job.getOutputs() == null) {
            return "";
        }

        Object output = getCallableResponseOutput(job)
            .orElseGet(() -> taskFileStorage.readJobOutputs(job.getOutputs()));

        return output == null ? "" : String.valueOf(output);
    }

    private Optional<Object> getCallableResponseOutput(Job job) {
        return taskExecutionService.fetchLastJobTaskExecution(Objects.requireNonNull(job.getId()))
            .filter(lastTaskExecution -> {
                Map<String, ?> metadata = lastTaskExecution.getMetadata();

                return metadata.containsKey(MetadataConstants.CALLABLE_RESPONSE);
            })
            .map(lastTaskExecution -> {
                ActionDefinition.CallableResponse callableResponse = ConvertUtils.convertValue(
                    taskFileStorage.readTaskExecutionOutput(lastTaskExecution.getOutput()),
                    ActionDefinition.CallableResponse.class);

                return callableResponse.output();
            });
    }

    private static A2ASkill toSkill(ExposedWorkflow exposedWorkflow) {
        A2aProjectWorkflow a2aProjectWorkflow = exposedWorkflow.a2aProjectWorkflow();

        Workflow workflow = exposedWorkflow.workflow();

        String skillName = a2aProjectWorkflow.getSkillName();
        String skillDescription = a2aProjectWorkflow.getSkillDescription();

        return new A2ASkill(
            getSkillId(exposedWorkflow), skillName == null ? Objects.toString(workflow.getLabel(), "") : skillName,
            skillDescription == null ? workflow.getDescription() : skillDescription, List.of());
    }

    private record ExposedWorkflow(
        A2aProjectWorkflow a2aProjectWorkflow, ProjectDeploymentWorkflow projectDeploymentWorkflow, Workflow workflow,
        String triggerName) {
    }
}
