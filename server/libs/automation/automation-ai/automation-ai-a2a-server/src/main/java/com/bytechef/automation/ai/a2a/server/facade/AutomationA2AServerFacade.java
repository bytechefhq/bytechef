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
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor.A2ASkill;
import com.bytechef.platform.ai.a2a.A2AAgentExecutor;
import com.bytechef.platform.ai.a2a.A2AAgentRequest;
import com.bytechef.platform.ai.a2a.A2AAgentResult;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.JobExecutionErrors;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges inbound A2A (Agent2Agent) requests onto ByteChef's synchronous workflow-execution path, and builds the
 * transport-agnostic {@link A2AAgentDescriptor} that backs the agent card.
 *
 * <p>
 * An A2A endpoint is addressed by an {@link A2aServer}'s {@code secretKey}. The server's exposed agent-backed workflows
 * (a project-deployment workflow carrying a {@code workflow/newWorkflowCall} trigger) become the card's skills. An
 * inbound {@code message/send} routes to the server's first such workflow: the message text is passed under the
 * conventional {@value #MESSAGE_INPUT} input keyed by the trigger name, the workflow runs synchronously, and its output
 * is returned as the agent's textual response.
 * </p>
 *
 * @author Ivica Cardic
 */
public class AutomationA2AServerFacade implements A2AAgentExecutor {

    static final String MESSAGE_INPUT = "message";

    private static final String AGENT_VERSION = "1.0.0";

    private static final Logger log = LoggerFactory.getLogger(AutomationA2AServerFacade.class);

    private final A2aProjectService a2aProjectService;
    private final A2aProjectWorkflowService a2aProjectWorkflowService;
    private final A2aServerService a2aServerService;
    private final JobCompletionAwaiter jobCompletionAwaiter;
    private final PrincipalJobFacade principalJobFacade;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AutomationA2AServerFacade(
        A2aProjectService a2aProjectService, A2aProjectWorkflowService a2aProjectWorkflowService,
        A2aServerService a2aServerService, JobCompletionAwaiter jobCompletionAwaiter,
        PrincipalJobFacade principalJobFacade, ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        this.a2aProjectService = a2aProjectService;
        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
        this.a2aServerService = a2aServerService;
        this.jobCompletionAwaiter = jobCompletionAwaiter;
        this.principalJobFacade = principalJobFacade;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    /**
     * Builds the agent descriptor for the A2A server addressed by {@code secretKey}, advertising each exposed
     * agent-backed workflow as a skill.
     *
     * @param secretKey the addressed A2A server's secret key
     * @param url       the absolute base URL at which this server's JSON-RPC endpoint is served
     * @return the descriptor used to render the agent card
     */
    public A2AAgentDescriptor getAgentDescriptor(String secretKey, String url) {
        A2aServer a2aServer = a2aServerService.getA2aServer(secretKey);

        List<A2ASkill> skills = new ArrayList<>();

        for (ExposedWorkflow exposedWorkflow : getExposedWorkflows(a2aServer)) {
            skills.add(toSkill(exposedWorkflow));
        }

        String description = a2aServer.getDescription();

        return new A2AAgentDescriptor(
            a2aServer.getName(), description == null ? "" : description, url, AGENT_VERSION, skills);
    }

    @Override
    public A2AAgentResult execute(A2AAgentRequest request) {
        A2aServer a2aServer = a2aServerService.getA2aServer(request.agentId());

        if (!a2aServer.isEnabled()) {
            return A2AAgentResult.ofError("A2A server is disabled");
        }

        List<ExposedWorkflow> exposedWorkflows = getExposedWorkflows(a2aServer);

        if (exposedWorkflows.isEmpty()) {
            return A2AAgentResult.ofError("No agent-backed workflow is exposed for this A2A server");
        }

        ExposedWorkflow exposedWorkflow = exposedWorkflows.getFirst();

        ProjectDeploymentWorkflow projectDeploymentWorkflow = exposedWorkflow.projectDeploymentWorkflow();

        Map<String, Object> inputs = new java.util.HashMap<>(projectDeploymentWorkflow.getInputs());

        inputs.put(exposedWorkflow.triggerName(), Map.of(MESSAGE_INPUT, request.text()));

        long jobId = principalJobFacade.createJob(
            new JobParametersDTO(projectDeploymentWorkflow.getWorkflowId(), inputs),
            projectDeploymentWorkflow.getProjectDeploymentId(), PlatformType.AUTOMATION);

        try {
            Job job = jobCompletionAwaiter.await(jobId, JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT)
                .join();

            JobExecutionErrors.checkForError(job, taskExecutionService);

            if (job.getOutputs() == null) {
                return A2AAgentResult.ofText("");
            }

            Object output = getCallableResponseOutput(job)
                .orElseGet(() -> taskFileStorage.readJobOutputs(job.getOutputs()));

            return A2AAgentResult.ofText(output == null ? "" : String.valueOf(output));
        } catch (Exception exception) {
            log.warn("A2A workflow execution failed for job {}: {}", jobId, exception.getMessage());

            return A2AAgentResult.ofError(exception.getMessage());
        }
    }

    private List<ExposedWorkflow> getExposedWorkflows(A2aServer a2aServer) {
        List<ExposedWorkflow> exposedWorkflows = new ArrayList<>();

        for (A2aProject a2aProject : a2aProjectService.getA2aServerA2aProjects(a2aServer.getId())) {
            for (A2aProjectWorkflow a2aProjectWorkflow : a2aProjectWorkflowService
                .getA2aProjectA2aProjectWorkflows(a2aProject.getId())) {

                ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
                    .getProjectDeploymentWorkflow(a2aProjectWorkflow.getProjectDeploymentWorkflowId());

                if (!projectDeploymentWorkflow.isEnabled()) {
                    continue;
                }

                Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

                WorkflowTrigger workflowTrigger = getNewWorkflowCallTrigger(workflow);

                if (workflowTrigger == null) {
                    continue;
                }

                exposedWorkflows.add(
                    new ExposedWorkflow(
                        a2aProjectWorkflow, projectDeploymentWorkflow, workflow, workflowTrigger.getName()));
            }
        }

        return exposedWorkflows;
    }

    private Optional<Object> getCallableResponseOutput(Job job) {
        try {
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
        } catch (Exception exception) {
            log.warn("Failed to extract callable response output from job {}: {}", job.getId(), exception.getMessage());

            return Optional.empty();
        }
    }

    private A2ASkill toSkill(ExposedWorkflow exposedWorkflow) {
        Map<String, ?> parameters = exposedWorkflow.a2aProjectWorkflow()
            .getParameters();

        Workflow workflow = exposedWorkflow.workflow();

        String skillName = asString(parameters.get(A2aProjectWorkflow.SKILL_NAME), workflow.getLabel());
        String skillDescription =
            asString(parameters.get(A2aProjectWorkflow.SKILL_DESCRIPTION), workflow.getDescription());

        Object tagsValue = parameters.get(A2aProjectWorkflow.SKILL_TAGS);

        List<String> tags = tagsValue instanceof List<?> list
            ? list.stream()
                .map(String::valueOf)
                .toList()
            : List.of();

        return new A2ASkill(workflow.getId(), skillName, skillDescription, tags);
    }

    private static String asString(@Nullable Object value, @Nullable String fallback) {
        if (value != null && !String.valueOf(value)
            .isBlank()) {

            return String.valueOf(value);
        }

        return fallback == null ? "" : fallback;
    }

    private static @Nullable WorkflowTrigger getNewWorkflowCallTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }

    private record ExposedWorkflow(
        A2aProjectWorkflow a2aProjectWorkflow, ProjectDeploymentWorkflow projectDeploymentWorkflow, Workflow workflow,
        String triggerName) {
    }
}
