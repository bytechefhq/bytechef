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

package com.bytechef.platform.coordinator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.coordinator.event.listener.ErrorWorkflowJobStatusApplicationEventListener;
import com.bytechef.platform.coordinator.event.listener.ErrorWorkflowPayloadFactory;
import com.bytechef.platform.coordinator.event.listener.ErrorWorkflowResolver;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import tools.jackson.databind.json.JsonMapper;

/**
 * End-to-end coverage for the error-workflow dispatch path: a real {@link ErrorWorkflowResolver} and a real
 * {@link ErrorWorkflowJobStatusApplicationEventListener} run against a real (in-memory) {@link JobService}, so the job
 * that gets created and the {@code errorHandlerFor} metadata it carries are genuinely read back out of the job store
 * rather than asserted against a mock invocation.
 * <p>
 * What is real: {@link ErrorWorkflowJobStatusApplicationEventListener}, {@link ErrorWorkflowResolver},
 * {@link ErrorWorkflowPayloadFactory}, {@link JobService} and {@link TaskExecutionService} (backed by the in-memory
 * atlas-execution repositories, not a database).
 * <p>
 * What is not real: {@link ProjectDeploymentService}, {@link ProjectService}, {@link ProjectWorkflowService},
 * {@link WorkflowService} and {@link PrincipalJobService} are Mockito mocks standing in for the JDBC-backed
 * automation-configuration services -- wiring a real database-backed instance of those into this module would mean
 * pulling automation-configuration-service's Liquibase schema and Testcontainers harness into a coordinator module that
 * has never needed either, which is a disproportionate cost for what this test needs to prove.
 * {@link PrincipalJobFacade} is a small recording fake that performs the one effect this test cares about -- persisting
 * the handler job through the shared, real {@link JobService} -- without pulling in the production facade's
 * plan-limit/rate-limit/licence admission chain, which is exercised by its own tests elsewhere.
 *
 * @author Ivica Cardic
 */
class ErrorWorkflowIntTest {

    private static final String FAILED_WORKFLOW_ID = "wf-failed";
    private static final String HANDLER_WORKFLOW_ID = "wf-handler";
    private static final String HANDLER_ERROR_TRIGGER_NAME = "newWorkflowError_1";
    private static final long PROJECT_ID = 1L;
    private static final long PROJECT_DEPLOYMENT_ID = 100L;
    private static final long FAILING_PROJECT_WORKFLOW_ID = 10L;
    private static final long HANDLER_PROJECT_WORKFLOW_ID = 20L;

    private final InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();
    private final JobService jobService = new JobServiceImpl(
        new InMemoryJobRepository(taskExecutionRepository, new JsonMapper()));
    private final TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

    private final ProjectDeploymentService projectDeploymentService = Mockito.mock(ProjectDeploymentService.class);
    private final ProjectService projectService = Mockito.mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = Mockito.mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = Mockito.mock(WorkflowService.class);
    private final PrincipalJobService principalJobService = Mockito.mock(PrincipalJobService.class);

    private final RecordingPrincipalJobFacade principalJobFacade = new RecordingPrincipalJobFacade();

    private final ErrorWorkflowResolver errorWorkflowResolver = new ErrorWorkflowResolver(
        projectDeploymentService, projectService, projectWorkflowService, workflowService);

    private final ErrorWorkflowJobStatusApplicationEventListener listener =
        new ErrorWorkflowJobStatusApplicationEventListener(
            new ErrorWorkflowPayloadFactory("https://app.example.com"), errorWorkflowResolver, jobService,
            principalJobFacade, principalJobService, taskExecutionService, null);

    /**
     * The single most important test here: a job that already carries {@code errorHandlerFor} metadata (i.e. it IS an
     * error-handler run) must dispatch nothing when it fails. Without this recursion cap, a persistently broken handler
     * workflow would spawn a new handler job every time it fails, forever.
     */
    @Test
    void testFailingHandlerDoesNotSpawnAnother() {
        long handlerJobId = givenFailedJobCarryingErrorHandlerForMetadata();

        listener.onApplicationEvent(new JobStatusApplicationEvent(handlerJobId, Job.Status.FAILED));

        assertEquals(
            0, principalJobFacade.createJobCallCount(),
            "a failing error workflow must not spawn another error workflow");

        // The recursion check runs first and returns before any of these collaborators are ever consulted.
        Mockito.verifyNoInteractions(
            projectDeploymentService, projectService, projectWorkflowService, workflowService, principalJobService);
    }

    /**
     * The positive path: a failed run whose project names a handler (here, an override on the failing project-workflow)
     * dispatches exactly one new job, and that job carries {@code errorHandlerFor} set to the failed job's id.
     */
    @Test
    void testFailedRunDispatchesTheHandler() {
        HandlerSetup handlerSetup = givenFailedJobWithConfiguredHandler();

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = stubHandlerErrorTrigger(
            handlerSetup.handlerWorkflow())) {

            listener.onApplicationEvent(new JobStatusApplicationEvent(handlerSetup.failedJobId(), Job.Status.FAILED));
        }

        assertEquals(1, principalJobFacade.createJobCallCount());

        Job handlerJob = jobService.getJob(principalJobFacade.lastCreatedJobId());

        assertEquals(
            String.valueOf(handlerSetup.failedJobId()), handlerJob.getMetadata(
                ErrorWorkflowJobStatusApplicationEventListener.ERROR_HANDLER_FOR));

        // The handler job's inputs must be namespaced under the handler workflow's error-trigger node name -- not
        // passed as top-level inputs -- so that editor data pills like ${newWorkflowError_1.execution.jobId}
        // actually resolve against the dispatched job.
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) handlerJob.getInputs()
            .get(HANDLER_ERROR_TRIGGER_NAME);

        assertNotNull(payload, "handler job inputs must be namespaced under the error trigger's node name");
        assertEquals(true, payload.containsKey("execution"));
    }

    /**
     * Persists a real FAILED job linked (via the mocked {@link PrincipalJobService}) to a project deployment whose
     * failing project-workflow carries an override pointing at a second project-workflow that exposes a
     * {@code workflow/newWorkflowError} trigger. Returns the failed job's id together with the mocked handler
     * {@link Workflow}, whose error-trigger name must additionally be stubbed via {@link #stubHandlerErrorTrigger}
     * before the returned failed job id is dispatched -- {@link WorkflowTrigger#of(Workflow)} is static and cannot be
     * stubbed with a plain {@code Mockito.when(...)} here.
     */
    private HandlerSetup givenFailedJobWithConfiguredHandler() {
        long failedJobId = persistFailedJob(FAILED_WORKFLOW_ID, Map.of());

        Mockito.when(principalJobService.fetchJobPrincipalId(failedJobId, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(PROJECT_DEPLOYMENT_ID));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(PROJECT_ID);
        projectDeployment.setEnvironment(Environment.STAGING);

        Mockito.when(projectDeploymentService.getProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(projectDeployment);

        ProjectWorkflow failingProjectWorkflow = new ProjectWorkflow(FAILING_PROJECT_WORKFLOW_ID);

        failingProjectWorkflow.setWorkflowId(FAILED_WORKFLOW_ID);
        failingProjectWorkflow.setErrorProjectWorkflowId(HANDLER_PROJECT_WORKFLOW_ID);
        failingProjectWorkflow.setErrorWorkflowDisabled(false);

        Mockito.when(projectWorkflowService.getWorkflowProjectWorkflow(FAILED_WORKFLOW_ID))
            .thenReturn(failingProjectWorkflow);

        ProjectWorkflow handlerProjectWorkflow = new ProjectWorkflow(HANDLER_PROJECT_WORKFLOW_ID);

        handlerProjectWorkflow.setWorkflowId(HANDLER_WORKFLOW_ID);

        Mockito.when(projectWorkflowService.getProjectWorkflow(HANDLER_PROJECT_WORKFLOW_ID))
            .thenReturn(handlerProjectWorkflow);

        // Workflow is a final, mostly-constructor-populated domain object with no label setter, so it is mocked
        // rather than instantiated directly -- the same approach ErrorWorkflowResolverTest takes.
        Workflow failedWorkflow = Mockito.mock(Workflow.class);

        Mockito.when(failedWorkflow.getLabel())
            .thenReturn("Failing Workflow");
        Mockito.when(workflowService.getWorkflow(FAILED_WORKFLOW_ID))
            .thenReturn(failedWorkflow);

        Workflow handlerWorkflow = Mockito.mock(Workflow.class);

        Mockito.when(workflowService.getWorkflow(HANDLER_WORKFLOW_ID))
            .thenReturn(handlerWorkflow);

        return new HandlerSetup(failedJobId, handlerWorkflow);
    }

    /**
     * Stubs the static {@link WorkflowTrigger#of(Workflow)} so {@code handlerWorkflow} exposes a single
     * {@code workflow/v1/newWorkflowError} trigger named {@link #HANDLER_ERROR_TRIGGER_NAME}. Must be used in a
     * try-with-resources block spanning the {@code listener.onApplicationEvent(...)} call it supports.
     */
    private static MockedStatic<WorkflowTrigger> stubHandlerErrorTrigger(Workflow handlerWorkflow) {
        WorkflowTrigger errorTrigger = Mockito.mock(WorkflowTrigger.class);

        Mockito.when(errorTrigger.getType())
            .thenReturn("workflow/v1/newWorkflowError");
        Mockito.when(errorTrigger.getName())
            .thenReturn(HANDLER_ERROR_TRIGGER_NAME);

        MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = Mockito.mockStatic(WorkflowTrigger.class);

        mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(handlerWorkflow))
            .thenReturn(List.of(errorTrigger));

        return mockedWorkflowTrigger;
    }

    /**
     * Pairs the persisted failed job's id with the mocked handler {@link Workflow}, so the caller can stub its
     * error-trigger name via {@link #stubHandlerErrorTrigger} before dispatching.
     */
    private record HandlerSetup(long failedJobId, Workflow handlerWorkflow) {
    }

    /**
     * Persists a real FAILED job that already carries {@code errorHandlerFor} metadata, as an error-handler run would.
     */
    private long givenFailedJobCarryingErrorHandlerForMetadata() {
        return persistFailedJob(
            HANDLER_WORKFLOW_ID, Map.of(ErrorWorkflowJobStatusApplicationEventListener.ERROR_HANDLER_FOR, "999"));
    }

    private long persistFailedJob(String workflowId, Map<String, ?> metadata) {
        Job job = new Job();

        job.setWorkflowId(workflowId);
        job.setStatus(Job.Status.FAILED);
        job.setMetadata(metadata);

        return Objects.requireNonNull(
            jobService.update(job)
                .getId());
    }

    /**
     * A minimal {@link PrincipalJobFacade} that performs only the effect this test cares about: persisting the handler
     * job through the shared, real {@link JobService}, exactly as {@code PrincipalJobFacadeImpl#createJob} does at its
     * core. The production facade's plan-limit, rate-limit and licence admission checks are deliberately not exercised
     * here -- they are covered by {@code PrincipalJobFacadeImplTest} and friends.
     */
    private final class RecordingPrincipalJobFacade implements PrincipalJobFacade {

        private int createJobCallCount;
        private Long lastCreatedJobId;

        int createJobCallCount() {
            return createJobCallCount;
        }

        Long lastCreatedJobId() {
            return lastCreatedJobId;
        }

        @Override
        public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO, PlatformType platformType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long createJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
            createJobCallCount++;

            Job job = new Job();

            job.setWorkflowId(jobParametersDTO.getWorkflowId());
            job.setInputs(jobParametersDTO.getInputs());
            job.setMetadata(jobParametersDTO.getMetadata());
            job.setStatus(Job.Status.CREATED);

            lastCreatedJobId = Objects.requireNonNull(
                jobService.update(job)
                    .getId());

            return lastCreatedJobId;
        }

        @Override
        public long createPrincipalLinkedJob(
            long referenceJobId, JobParametersDTO jobParametersDTO, PlatformType platformType) {

            throw new UnsupportedOperationException();
        }

        @Override
        public Job createJobWithoutDispatch(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
            throw new UnsupportedOperationException();
        }
    }
}
