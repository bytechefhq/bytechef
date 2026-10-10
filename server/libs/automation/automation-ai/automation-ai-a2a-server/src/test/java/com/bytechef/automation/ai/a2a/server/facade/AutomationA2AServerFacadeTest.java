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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
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
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor.A2ASkill;
import com.bytechef.platform.ai.a2a.A2AAgentRequest;
import com.bytechef.platform.ai.a2a.A2AAgentResult;
import com.bytechef.platform.ai.a2a.A2AAgentResult.Completed;
import com.bytechef.platform.ai.a2a.A2AAgentResult.Failed;
import com.bytechef.platform.ai.a2a.A2AAgentResult.InputRequired;
import com.bytechef.platform.ai.a2a.A2AAgentResult.Working;
import com.bytechef.platform.ai.a2a.A2AAgentRun;
import com.bytechef.platform.ai.a2a.A2AInvalidParamsException;
import com.bytechef.platform.ai.a2a.A2ATaskReference;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AutomationA2AServerFacadeTest {

    private static final String SECRET_KEY = "secret";

    private final A2aProjectService a2aProjectService = mock(A2aProjectService.class);
    private final A2aProjectWorkflowService a2aProjectWorkflowService = mock(A2aProjectWorkflowService.class);
    private final A2aServerService a2aServerService = mock(A2aServerService.class);
    private final JobCompletionAwaiter jobCompletionAwaiter = mock(JobCompletionAwaiter.class);
    private final JobService jobService = mock(JobService.class);
    private final PrincipalJobFacade principalJobFacade = mock(PrincipalJobFacade.class);
    private final PrincipalJobService principalJobService = mock(PrincipalJobService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TaskFileStorage taskFileStorage = mock(TaskFileStorage.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<ApprovalTokens> approvalTokensObjectProvider =
        (ObjectProvider<ApprovalTokens>) mock(ObjectProvider.class);

    private final AutomationA2AServerFacade facade = createFacade(Runnable::run);

    private final List<A2aProjectWorkflow> exposedA2aProjectWorkflows = new ArrayList<>();

    @Test
    void testStartReturnsFailedWhenServerDisabled() {
        A2aServer a2aServer = new A2aServer("agent", null, Environment.DEVELOPMENT);

        a2aServer.setEnabled(false);

        when(a2aServerService.getA2aServer(SECRET_KEY)).thenReturn(a2aServer);

        A2AAgentRun agentRun = facade.start(request(null));

        assertThat(agentRun.taskReference()).isNull();
        assertThat(agentRun.result()
            .join()).isInstanceOfSatisfying(
                Failed.class, failed -> assertThat(failed.errorMessage()).contains("disabled"));
    }

    @Test
    void testStartReturnsFailedWhenNoWorkflowExposed() {
        stubServer();

        A2AAgentRun agentRun = facade.start(request(null));

        assertThat(agentRun.result()
            .join()).isInstanceOfSatisfying(
                Failed.class, failed -> assertThat(failed.errorMessage()).contains("No enabled workflow"));
        verify(principalJobFacade, never()).createJob(any(), anyLong(), any());
    }

    @Test
    void testStartCreatesTheJobWithTheMessageAndADurableTaskReference() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of("deploymentInput", "value"));

        A2AAgentRun agentRun = start(CompletableFuture.completedFuture(completedJob(100L)), null);

        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
            ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(principalJobFacade).createJob(
            jobParametersDTOArgumentCaptor.capture(), eq(30L), eq(PlatformType.AUTOMATION));

        JobParametersDTO jobParametersDTO = jobParametersDTOArgumentCaptor.getValue();

        assertThat(jobParametersDTO.getInputs())
            .containsEntry("newWorkflowCall_1", Map.of(AutomationA2AServerFacade.MESSAGE_INPUT, "hi"))
            .containsEntry(JobInputConstants.TRIGGER_NAME_INPUT, "newWorkflowCall_1")
            .containsEntry("deploymentInput", "value");

        Map<String, Object> metadata = jobParametersDTO.getMetadata();

        assertThat(metadata).containsEntry(AutomationA2AServerFacade.A2A_CONTEXT_ID_METADATA_KEY, "context-1");

        A2ATaskReference taskReference = agentRun.taskReference();

        assertThat(taskReference).isNotNull();
        assertThat(taskReference.contextId()).isEqualTo("context-1");

        A2aTaskId a2aTaskId = A2aTaskId.parse(taskReference.taskId())
            .orElseThrow();

        assertThat(a2aTaskId.jobId()).isEqualTo(100L);
        assertThat(a2aTaskId.nonce()
            .toString()).isEqualTo(metadata.get(AutomationA2AServerFacade.A2A_TASK_NONCE_METADATA_KEY));
    }

    @Test
    void testPausedRunIsInputRequiredAndItsTaskIdIsNotTheResumeToken() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        Job job = mock(Job.class);
        String jobResumeId = JobResumeId.of(100L)
            .toString();

        when(job.getId()).thenReturn(100L);
        when(job.getStatus()).thenReturn(Job.Status.STOPPED);
        when(job.getMetadata(MetadataConstants.JOB_RESUME_ID)).thenReturn(jobResumeId);

        A2AAgentRun agentRun = start(CompletableFuture.completedFuture(job), null);

        A2AAgentResult agentResult = agentRun.result()
            .join();

        assertThat(agentResult).isInstanceOfSatisfying(
            InputRequired.class, inputRequired -> assertThat(inputRequired.text()).contains("Approval required"));
        assertThat(agentResult.taskReference()).isEqualTo(agentRun.taskReference());
        assertThat(agentRun.taskReference()
            .taskId()).isNotEqualTo(jobResumeId)
                .doesNotContain(jobResumeId);
    }

    @Test
    void testFailedRunDoesNotDiscloseTheTaskErrorToTheCaller() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        Job job = mock(Job.class);

        when(job.getId()).thenReturn(100L);
        when(job.getStatus()).thenReturn(Job.Status.FAILED);

        TaskExecution taskExecution = mock(TaskExecution.class);

        when(taskExecution.getStatus()).thenReturn(TaskExecution.Status.FAILED);
        when(taskExecution.getError()).thenReturn(
            new ExecutionError("jdbc:postgresql://internal-host:5432/db refused", List.of()));
        when(taskExecutionService.fetchLastJobTaskExecution(100L)).thenReturn(Optional.of(taskExecution));

        A2AAgentResult agentResult = start(CompletableFuture.completedFuture(job), null).result()
            .join();

        assertThat(agentResult).isInstanceOfSatisfying(Failed.class, failed -> {
            assertThat(failed.errorMessage()).contains("100")
                .doesNotContain("internal-host");
            assertThat(failed.taskReference()).isNotNull();
        });
    }

    @Test
    void testRunStoppedWithoutApprovalIsFailed() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        Job job = mock(Job.class);

        when(job.getId()).thenReturn(100L);
        when(job.getStatus()).thenReturn(Job.Status.STOPPED);

        assertThat(start(CompletableFuture.completedFuture(job), null).result()
            .join()).isInstanceOfSatisfying(
                Failed.class, failed -> assertThat(failed.errorMessage()).contains("did not complete"));
    }

    @Test
    void testStartRunsTheWorkflowOfTheRequestedSkill() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());
        stubExposedWorkflow("wf2", 21L, Map.of());

        start(CompletableFuture.completedFuture(completedJob(100L)), "wf2");

        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
            ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(principalJobFacade).createJob(jobParametersDTOArgumentCaptor.capture(), anyLong(), any());

        assertThat(jobParametersDTOArgumentCaptor.getValue()
            .getWorkflowId()).isEqualTo("wf2");
    }

    @Test
    void testStartRejectsAnUnknownSkillWithInvalidParams() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        assertThatExceptionOfType(A2AInvalidParamsException.class)
            .isThrownBy(() -> start(CompletableFuture.completedFuture(completedJob(100L)), "unknown"))
            .withMessageContaining("unknown")
            .withMessageContaining("wf1");
        verify(principalJobFacade, never()).createJob(any(), anyLong(), any());
    }

    @Test
    void testStartRequiresASkillIdWhenSeveralSkillsAreExposed() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());
        stubExposedWorkflow("wf2", 21L, Map.of());

        assertThatExceptionOfType(A2AInvalidParamsException.class)
            .isThrownBy(() -> start(CompletableFuture.completedFuture(completedJob(100L)), null))
            .withMessageContaining("wf1, wf2");
        verify(principalJobFacade, never()).createJob(any(), anyLong(), any());
    }

    @Test
    void testTimedOutWaitReturnsWorkingWithTheDurableTaskReference() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        A2AAgentRun agentRun = start(CompletableFuture.failedFuture(new TimeoutException()), null);

        assertThat(agentRun.result()
            .join()).isInstanceOfSatisfying(
                Working.class,
                working -> assertThat(working.taskReference()).isEqualTo(agentRun.taskReference()));
    }

    @Test
    void testFailedWaitKeepsTheRunPollable() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        A2AAgentRun agentRun = start(CompletableFuture.failedFuture(new IllegalStateException("broker down")), null);

        assertThat(agentRun.result()
            .join()).isInstanceOf(Working.class);
    }

    @Test
    void testResultCompletesOnlyWhenTheRunCompletes() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        CompletableFuture<Job> jobFuture = new CompletableFuture<>();

        A2AAgentRun agentRun = start(jobFuture, null);

        assertThat(agentRun.result()).isNotDone();

        jobFuture.complete(completedJob(100L));

        assertThat(agentRun.result()
            .join()).isInstanceOf(Completed.class);
    }

    @Test
    void testCompletedRunReturnsTheCallableResponseOutput() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        FileEntry outputFileEntry = mock(FileEntry.class);
        TaskExecution taskExecution = mock(TaskExecution.class);

        when(taskExecution.getMetadata()).thenAnswer(invocation -> Map.of(MetadataConstants.CALLABLE_RESPONSE, true));
        when(taskExecution.getOutput()).thenReturn(outputFileEntry);
        when(taskExecutionService.fetchLastJobTaskExecution(100L)).thenReturn(Optional.of(taskExecution));
        when(taskFileStorage.readTaskExecutionOutput(outputFileEntry)).thenReturn(Map.of("output", "the answer"));

        A2AAgentResult agentResult = start(CompletableFuture.completedFuture(completedJob(100L)), null).result()
            .join();

        assertThat(agentResult).isInstanceOfSatisfying(
            Completed.class, completed -> assertThat(completed.text()).isEqualTo("the answer"));
        verify(taskFileStorage, never()).readJobOutputs(any());
    }

    @Test
    void testCompletedRunFallsBackToTheJobOutputsWithoutACallableResponse() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        Job job = completedJob(100L);

        when(taskExecutionService.fetchLastJobTaskExecution(100L)).thenReturn(Optional.empty());
        when(taskFileStorage.readJobOutputs(job.getOutputs())).thenAnswer(invocation -> Map.of("result", 42));

        assertThat(start(CompletableFuture.completedFuture(job), null).result()
            .join()).isInstanceOfSatisfying(
                Completed.class, completed -> assertThat(completed.text()).isEqualTo("{\"result\":42}"));
    }

    @Test
    void testCompletedRunReturnsTheTextOfASingleMessageResponse() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        FileEntry outputFileEntry = mock(FileEntry.class);
        TaskExecution taskExecution = mock(TaskExecution.class);

        when(taskExecution.getMetadata()).thenAnswer(invocation -> Map.of(MetadataConstants.CALLABLE_RESPONSE, true));
        when(taskExecution.getOutput()).thenReturn(outputFileEntry);
        when(taskExecutionService.fetchLastJobTaskExecution(100L)).thenReturn(Optional.of(taskExecution));
        when(taskFileStorage.readTaskExecutionOutput(outputFileEntry)).thenReturn(
            Map.of("output", Map.of("message", "Java 25 is an LTS release.")));

        A2AAgentResult agentResult = start(CompletableFuture.completedFuture(completedJob(100L)), null).result()
            .join();

        assertThat(agentResult).isInstanceOfSatisfying(
            Completed.class, completed -> assertThat(completed.text()).isEqualTo("Java 25 is an LTS release."));
    }

    @Test
    void testToOutputTextWritesStructuredOutputAsJson() {
        assertThat(AutomationA2AServerFacade.toOutputText(null)).isEmpty();
        assertThat(AutomationA2AServerFacade.toOutputText("plain")).isEqualTo("plain");
        assertThat(AutomationA2AServerFacade.toOutputText(Map.of("message", "hello"))).isEqualTo("hello");
        assertThat(AutomationA2AServerFacade.toOutputText(Map.of("count", 3))).isEqualTo("{\"count\":3}");
        assertThat(AutomationA2AServerFacade.toOutputText(List.of("a", "b"))).isEqualTo("[\"a\",\"b\"]");
    }

    @Test
    void testUnreadableResultKeepsTheRunPollableInsteadOfFailingIt() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        TaskExecution taskExecution = mock(TaskExecution.class);

        when(taskExecution.getMetadata()).thenAnswer(invocation -> Map.of(MetadataConstants.CALLABLE_RESPONSE, true));
        when(taskExecutionService.fetchLastJobTaskExecution(100L)).thenReturn(Optional.of(taskExecution));
        when(taskFileStorage.readTaskExecutionOutput(any())).thenThrow(new IllegalStateException("missing blob"));

        A2AAgentRun agentRun = start(CompletableFuture.completedFuture(completedJob(100L)), null);

        assertThat(agentRun.result()
            .join()).isInstanceOfSatisfying(
                Working.class,
                working -> assertThat(working.taskReference()).isEqualTo(agentRun.taskReference()));
        verify(taskFileStorage, never()).readJobOutputs(any());
    }

    @Test
    void testUnreadableErrorStateKeepsTheRunPollableInsteadOfFailingIt() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        Job job = mock(Job.class);

        when(job.getId()).thenReturn(100L);
        when(job.getStatus()).thenReturn(Job.Status.FAILED);
        when(taskExecutionService.fetchLastJobTaskExecution(100L))
            .thenThrow(new IllegalStateException("database down"));

        assertThat(start(CompletableFuture.completedFuture(job), null).result()
            .join()).isInstanceOf(Working.class);
    }

    @Test
    void testPollTaskPropagatesAnUnreadableResult() {
        String taskId = stubJobOfTask(7L, 30L, Job.Status.COMPLETED);

        Job job = jobService.fetchJob(7L)
            .orElseThrow();

        when(job.getOutputs()).thenReturn(mock(FileEntry.class));
        when(taskExecutionService.fetchLastJobTaskExecution(7L)).thenReturn(Optional.empty());
        when(taskFileStorage.readJobOutputs(any())).thenThrow(new IllegalStateException("missing blob"));

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> facade.pollTask(SECRET_KEY, taskId));
    }

    @Test
    void testAwaitFailureAfterTheJobIsCreatedKeepsTheRunPollable() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(100L);
        when(jobCompletionAwaiter.await(anyLong(), any())).thenThrow(new IllegalStateException("database down"));

        A2AAgentRun agentRun = facade.start(request(null));

        assertThat(agentRun.taskReference()).isNotNull();
        assertThat(agentRun.result()
            .join()).isInstanceOfSatisfying(
                Working.class,
                working -> assertThat(working.taskReference()).isEqualTo(agentRun.taskReference()));
    }

    @Test
    void testRunResultIsReadOnTheCompletionExecutorUnderTheCallersTenant() throws InterruptedException {
        AutomationA2AServerFacade threadedFacade = createFacade(
            runnable -> new Thread(runnable, "a2a-run-completion-test").start());

        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        AtomicReference<String> readingTenantId = new AtomicReference<>();
        AtomicReference<String> readingThreadName = new AtomicReference<>();

        Job job = completedJob(100L);

        when(taskExecutionService.fetchLastJobTaskExecution(100L)).thenAnswer(invocation -> {
            Thread currentThread = Thread.currentThread();

            readingTenantId.set(TenantContext.getCurrentTenantId());
            readingThreadName.set(currentThread.getName());

            return Optional.empty();
        });
        when(taskFileStorage.readJobOutputs(job.getOutputs())).thenAnswer(invocation -> Map.of("answer", 42));

        CompletableFuture<Job> jobFuture = new CompletableFuture<>();

        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(100L);
        when(jobCompletionAwaiter.await(anyLong(), any())).thenReturn(jobFuture);

        A2AAgentRun agentRun = TenantContext.callWithTenantId("tenant-1", () -> threadedFacade.start(request(null)));

        Thread brokerThread = new Thread(
            () -> TenantContext.runWithTenantId("tenant-2", () -> jobFuture.complete(job)), "broker-thread");

        brokerThread.start();
        brokerThread.join();

        assertThat(agentRun.result()
            .join()).isInstanceOfSatisfying(
                Completed.class, completed -> assertThat(completed.text()).isEqualTo("{\"answer\":42}"));
        assertThat(readingTenantId.get()).isEqualTo("tenant-1");
        assertThat(readingThreadName.get()).isEqualTo("a2a-run-completion-test");
    }

    @Test
    void testPollTaskReturnsTheRunWhenTheJobBelongsToTheServer() {
        String taskId = stubJobOfTask(7L, 30L, Job.Status.COMPLETED);

        A2AAgentResult agentResult = facade.pollTask(SECRET_KEY, taskId);

        assertThat(agentResult).isInstanceOf(Completed.class);
        assertThat(agentResult.taskReference()).isEqualTo(new A2ATaskReference(taskId, "context-7"));
    }

    @Test
    void testPollTaskReturnsWorkingWhileTheRunIsInProgress() {
        String taskId = stubJobOfTask(7L, 30L, Job.Status.STARTED);

        assertThat(facade.pollTask(SECRET_KEY, taskId)).isInstanceOf(Working.class);
    }

    @Test
    void testPollTaskReturnsNullWhenTheJobBelongsToAnotherDeployment() {
        String taskId = stubJobOfTask(7L, 99L, Job.Status.COMPLETED);

        assertThat(facade.pollTask(SECRET_KEY, taskId)).isNull();
    }

    @Test
    void testPollTaskReturnsNullWhenTheNonceDoesNotMatchTheJob() {
        stubJobOfTask(7L, 30L, Job.Status.COMPLETED);

        String forgedTaskId = new A2aTaskId(7L, UUID.randomUUID()).asString();

        assertThat(facade.pollTask(SECRET_KEY, forgedTaskId)).isNull();
    }

    @Test
    void testPollTaskReturnsNullWhenTheJobIsMissing() {
        String taskId = new A2aTaskId(8L, UUID.randomUUID()).asString();

        when(jobService.fetchJob(8L)).thenReturn(Optional.empty());

        assertThat(facade.pollTask(SECRET_KEY, taskId)).isNull();
    }

    @Test
    void testPollTaskRejectsResumeTokensAndUnparseableIds() {
        assertThat(facade.pollTask(SECRET_KEY, JobResumeId.of(7L)
            .toString())).isNull();
        assertThat(facade.pollTask(SECRET_KEY, "not-a-task-id")).isNull();
        verify(jobService, never()).fetchJob(anyLong());
    }

    @Test
    void testAgentDescriptorUsesSkillOverridesAndFallsBackToTheWorkflow() {
        stubServer();

        A2aProjectWorkflow overriddenA2aProjectWorkflow = stubExposedWorkflow("wf1", 20L, Map.of());

        overriddenA2aProjectWorkflow.setSkillName("Summarize");
        overriddenA2aProjectWorkflow.setSkillDescription("Summarizes text");

        stubExposedWorkflow("wf2", 21L, Map.of());

        A2AAgentDescriptor agentDescriptor = facade.getAgentDescriptor(SECRET_KEY, "https://example.com/a2a");

        assertThat(agentDescriptor.skills()).containsExactly(
            new A2ASkill("wf1", "Summarize", "Summarizes text", List.of()),
            new A2ASkill("wf2", "Label wf2", "Description wf2", List.of()));
    }

    @Test
    void testAgentDescriptorSkipsDisabledWorkflowsAndWorkflowsWithoutACallTrigger() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        ProjectDeploymentWorkflow disabledProjectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(disabledProjectDeploymentWorkflow.isEnabled()).thenReturn(false);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(40L))
            .thenReturn(disabledProjectDeploymentWorkflow);

        exposedA2aProjectWorkflows.add(new A2aProjectWorkflow(10L, 40L));

        stubExposedWorkflowWithTriggers("wf3", 41L, "[]");

        A2AAgentDescriptor agentDescriptor = facade.getAgentDescriptor(SECRET_KEY, "https://example.com/a2a");

        assertThat(agentDescriptor.skills()).extracting(A2ASkill::id)
            .containsExactly("wf1");
    }

    @Test
    void testDisabledA2aProjectWorkflowIsNotExposedOrRunnable() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        A2aProjectWorkflow disabledA2aProjectWorkflow = stubExposedWorkflow("wf2", 21L, Map.of());

        disabledA2aProjectWorkflow.setEnabled(false);

        A2AAgentDescriptor agentDescriptor = facade.getAgentDescriptor(SECRET_KEY, "https://example.com/a2a");

        assertThat(agentDescriptor.skills()).extracting(A2ASkill::id)
            .containsExactly("wf1");

        assertThatExceptionOfType(A2AInvalidParamsException.class)
            .isThrownBy(() -> start(CompletableFuture.completedFuture(completedJob(100L)), "wf2"))
            .withMessageContaining("wf2");
        verify(principalJobFacade, never()).createJob(any(), anyLong(), any());
    }

    @Test
    void testEnabledA2aProjectWorkflowIsRunnableWhenAnotherIsDisabled() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());

        A2aProjectWorkflow disabledA2aProjectWorkflow = stubExposedWorkflow("wf2", 21L, Map.of());

        disabledA2aProjectWorkflow.setEnabled(false);

        start(CompletableFuture.completedFuture(completedJob(100L)), null);

        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
            ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(principalJobFacade).createJob(jobParametersDTOArgumentCaptor.capture(), anyLong(), any());

        assertThat(jobParametersDTOArgumentCaptor.getValue()
            .getWorkflowId()).isEqualTo("wf1");
    }

    @Test
    void testWorkflowWithAnotherTriggerTypeIsNotExposedOrRunnable() {
        stubServer();
        stubExposedWorkflow("wf1", 20L, Map.of());
        stubExposedWorkflowWithTriggers(
            "wf4", 42L, "[{\"name\": \"webhook_1\", \"type\": \"webhook/v1/autoRespondingWithHTTP200\"}]");

        A2AAgentDescriptor agentDescriptor = facade.getAgentDescriptor(SECRET_KEY, "https://example.com/a2a");

        assertThat(agentDescriptor.skills()).extracting(A2ASkill::id)
            .containsExactly("wf1");

        assertThatExceptionOfType(A2AInvalidParamsException.class)
            .isThrownBy(() -> start(CompletableFuture.completedFuture(completedJob(100L)), "wf4"))
            .withMessageContaining("wf4");
        verify(principalJobFacade, never()).createJob(any(), anyLong(), any());
    }

    private static Job completedJob(long jobId) {
        Job job = mock(Job.class);

        when(job.getId()).thenReturn(jobId);
        when(job.getStatus()).thenReturn(Job.Status.COMPLETED);
        when(job.getOutputs()).thenReturn(mock(FileEntry.class));

        return job;
    }

    private static A2AAgentRequest request(@Nullable String skillId) {
        return new A2AAgentRequest(SECRET_KEY, "hi", "context-1", skillId);
    }

    private A2AAgentRun start(CompletableFuture<Job> jobFuture, @Nullable String skillId) {
        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(100L);
        when(jobCompletionAwaiter.await(anyLong(), any())).thenReturn(jobFuture);

        return facade.start(request(skillId));
    }

    private AutomationA2AServerFacade createFacade(Executor runCompletionExecutor) {
        return new AutomationA2AServerFacade(
            a2aProjectService, a2aProjectWorkflowService, a2aServerService, approvalTokensObjectProvider,
            jobCompletionAwaiter, jobService, principalJobFacade, principalJobService, projectDeploymentWorkflowService,
            "https://example.com", runCompletionExecutor, taskExecutionService, taskFileStorage, workflowService);
    }

    private String stubJobOfTask(long jobId, long projectDeploymentId, Job.Status status) {
        UUID nonce = UUID.randomUUID();

        Job job = mock(Job.class);

        when(job.getId()).thenReturn(jobId);
        when(job.getStatus()).thenReturn(status);
        when(job.getMetadata(AutomationA2AServerFacade.A2A_TASK_NONCE_METADATA_KEY)).thenReturn(nonce.toString());
        when(job.getMetadata(AutomationA2AServerFacade.A2A_CONTEXT_ID_METADATA_KEY)).thenReturn("context-" + jobId);
        when(jobService.fetchJob(jobId)).thenReturn(Optional.of(job));
        when(principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(projectDeploymentId));

        stubServer();

        when(a2aProjectService.getA2aServerA2aProjects(1L)).thenReturn(List.of(new A2aProject(30L, 1L, 42L)));

        return new A2aTaskId(jobId, nonce).asString();
    }

    private void stubServer() {
        A2aServer a2aServer = new A2aServer("agent", null, Environment.DEVELOPMENT);

        a2aServer.setId(1L);

        A2aProject a2aProject = new A2aProject(30L, 1L, 42L);

        a2aProject.setId(10L);

        when(a2aServerService.getA2aServer(SECRET_KEY)).thenReturn(a2aServer);
        when(a2aProjectService.getA2aServerA2aProjects(1L)).thenReturn(List.of(a2aProject));
        when(a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(10L)).thenReturn(exposedA2aProjectWorkflows);
    }

    private A2aProjectWorkflow stubExposedWorkflow(
        String workflowId, long projectDeploymentWorkflowId, Map<String, ?> deploymentInputs) {

        stubWorkflow(
            workflowId, projectDeploymentWorkflowId, deploymentInputs,
            "[{\"name\": \"newWorkflowCall_1\", \"type\": \"workflow/v1/newWorkflowCall\"}]");

        A2aProjectWorkflow a2aProjectWorkflow = new A2aProjectWorkflow(10L, projectDeploymentWorkflowId);

        exposedA2aProjectWorkflows.add(a2aProjectWorkflow);

        return a2aProjectWorkflow;
    }

    private void stubExposedWorkflowWithTriggers(
        String workflowId, long projectDeploymentWorkflowId, String triggersJson) {

        stubWorkflow(workflowId, projectDeploymentWorkflowId, Map.of(), triggersJson);

        exposedA2aProjectWorkflows.add(new A2aProjectWorkflow(10L, projectDeploymentWorkflowId));
    }

    private void stubWorkflow(
        String workflowId, long projectDeploymentWorkflowId, Map<String, ?> deploymentInputs, String triggersJson) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(projectDeploymentWorkflow.isEnabled()).thenReturn(true);
        when(projectDeploymentWorkflow.getWorkflowId()).thenReturn(workflowId);
        when(projectDeploymentWorkflow.getProjectDeploymentId()).thenReturn(30L);
        when(projectDeploymentWorkflow.getInputs()).thenAnswer(invocation -> deploymentInputs);
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflowId))
            .thenReturn(projectDeploymentWorkflow);

        String definition = "{\"label\": \"Label " + workflowId + "\", \"description\": \"Description " +
            workflowId + "\", \"triggers\": " + triggersJson + ", \"tasks\": []}";

        Workflow workflow = new Workflow(workflowId, definition, Workflow.Format.JSON);

        when(workflowService.getWorkflow(workflowId)).thenReturn(workflow);
    }
}
