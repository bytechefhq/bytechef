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

package com.bytechef.component.ai.agent;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.facade.AiAgentToolFacade;
import com.bytechef.component.ai.agent.tool.AgentToolSuspension;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.test.config.ComponentTestIntConfiguration;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.ApprovalFormFacadeImpl;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacadeImpl;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.task.dispatcher.suspend.SuspendTaskDispatcherPreSendProcessor;
import com.bytechef.task.dispatcher.suspend.completion.SuspendTaskCompletionHandler;
import com.bytechef.tenant.TenantContext;
import io.micrometer.tracing.Tracer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ComponentTestIntConfiguration.class,
        AiAgentToolSuspensionIntTest.AiAgentToolSuspensionIntTestConfiguration.class
    },
    properties = {
        "bytechef.file-storage.provider=jdbc", "bytechef.public-url=http://localhost:9555",
        "bytechef.workflow.repository.classpath.enabled=true"
    })
class AiAgentToolSuspensionIntTest {

    private static final String APPROVAL_TOOL_NAME = "requestApproval";
    private static final String FINAL_ANSWER = "The refund was approved and issued.";
    private static final long MODEL_CONNECTION_ID = 1L;
    private static final String QUESTION_WORKFLOW_ID = Base64.getEncoder()
        .encodeToString("aiagent_v1_question".getBytes(StandardCharsets.UTF_8));
    private static final String WORKFLOW_ID = Base64.getEncoder()
        .encodeToString("aiagent_v1_approval".getBytes(StandardCharsets.UTF_8));

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScriptedChatModel scriptedChatModel;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private Map<String, TaskHandler<?>> taskHandlerMap;

    @Autowired
    private WorkflowService workflowService;

    private ApplicationEventPublisher eventPublisher;
    private JobService jobService;
    private JobSyncExecutor jobSyncExecutor;
    private JobResumeFacade jobResumeFacade;
    private TaskExecutionService taskExecutionService;
    private TaskFileStorage taskFileStorage;
    private TaskStateService taskStateService;

    @BeforeEach
    void beforeEach() {
        scriptedChatModel.reset();

        Connection connection = new Connection();

        connection.setComponentName("testModel");
        connection.setConnectionVersion(1);
        connection.setId(MODEL_CONNECTION_ID);
        connection.setParameters(Map.of());

        when(connectionService.getConnection(MODEL_CONNECTION_ID)).thenReturn(connection);

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();
        taskStateService = new InMemoryTaskStateService();
        Evaluator evaluator = SpelEvaluator.create();

        jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);
        taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

        AsyncMessageBroker asyncMessageBroker = new AsyncMessageBroker(environment);

        eventPublisher = createEventPublisher(asyncMessageBroker);

        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors = List.of(
            new SuspendTaskDispatcherPreSendProcessor(jobService, taskStateService, null),
            new ModelConnectionTaskDispatcherPreSendProcessor());

        jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, jobService, -1, asyncMessageBroker, List.of(),
            List.of(
                (taskCompletionHandler, taskDispatcher) -> new SuspendTaskCompletionHandler(
                    contextService, eventPublisher, jobService, taskExecutionService, taskFileStorage,
                    taskStateService)),
            List.of(), taskDispatcherPreSendProcessors, List.of(), taskExecutionService, taskExecutor,
            taskHandlerMap::get, taskFileStorage, -1, workflowService);

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            List.of(new DefaultTaskDispatcher(eventPublisher, taskDispatcherPreSendProcessors)));

        TaskCoordinator taskCoordinator = new TaskCoordinator(
            List.of(), List.of(), eventPublisher,
            new JobExecutor(
                contextService, evaluator, taskDispatcherChain, taskExecutionService, taskFileStorage, workflowService),
            jobService, new TaskCompletionHandlerChain(), taskDispatcherChain, taskExecutionService);

        asyncMessageBroker.receive(
            TaskCoordinatorMessageRoute.JOB_RESUME_EVENTS, message -> {
                ResumeJobEvent resumeJobEvent = (ResumeJobEvent) message;

                Long taskExecutionId = resumeJobEvent.getTaskExecutionId();

                if (taskExecutionId != null) {
                    clearTransientStateLikeADatabaseReload(taskExecutionId);
                }

                taskCoordinator.onResumeJobEvent(resumeJobEvent);
            });

        jobResumeFacade = new JobResumeFacadeImpl(
            event -> {},
            new JobFacadeImpl(
                eventPublisher, contextService, jobService, taskExecutionService, taskFileStorage, workflowService),
            jobService);
    }

    @Test
    void testApprovalSuspendsTheAgentAndResumeContinuesWithTheApproversAnswer() {
        Job suspendedJob = jobSyncExecutor.execute(new JobParametersDTO(WORKFLOW_ID, Map.of()), false);

        assertThat(suspendedJob.getStatus()).isEqualTo(Job.Status.STOPPED);
        assertThat(scriptedChatModel.getPrompts()).hasSize(1);

        TaskExecution suspendedTaskExecution = getAgentTaskExecution(suspendedJob);

        Object suspendOutput = taskFileStorage.readTaskExecutionOutput(
            Objects.requireNonNull(suspendedTaskExecution.getOutput()));

        assertThat(suspendOutput.toString()).contains(AgentToolSuspension.CONTINUE_PARAMETER_KEY);

        JobResumeOutcome jobResumeOutcome = jobResumeFacade.resumeJob(
            getJobResumeId(suspendedJob), Map.of("approved", true));

        assertThat(jobResumeOutcome).isEqualTo(JobResumeOutcome.OK);

        Job completedJob = awaitJobStatus(Objects.requireNonNull(suspendedJob.getId()), Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(Objects.requireNonNull(completedJob.getOutputs()));

        assertThat(outputs.get("answer")).isEqualTo(FINAL_ANSWER);

        List<Prompt> prompts = scriptedChatModel.getPrompts();

        assertThat(prompts).hasSize(2);
        assertThat(getApprovalToolResponse(prompts.get(1))).isEqualTo("{\"approved\":true}");
    }

    @Test
    void testApprovalFormShowsTheApprovalToolParametersNotTheAgentParameters() {
        Job suspendedJob = jobSyncExecutor.execute(new JobParametersDTO(WORKFLOW_ID, Map.of()), false);

        assertThat(suspendedJob.getStatus()).isEqualTo(Job.Status.STOPPED);

        ApprovalFormFacadeImpl approvalFormFacade = new ApprovalFormFacadeImpl(
            jobService, taskExecutionService, taskStateService);

        Map<String, ?> approvalForm = approvalFormFacade.getApprovalForm(getJobResumeId(suspendedJob));

        assertThat(approvalForm.get("formTitle")).isEqualTo("Approve the refund of order 42");
        assertThat(approvalForm).doesNotContainKey("userPrompt");
    }

    @Test
    void testSuspendTimeoutResumesTheAgentWithNoResponse() {
        Job suspendedJob = jobSyncExecutor.execute(new JobParametersDTO(WORKFLOW_ID, Map.of()), false);

        assertThat(suspendedJob.getStatus()).isEqualTo(Job.Status.STOPPED);

        long jobId = Objects.requireNonNull(suspendedJob.getId());

        eventPublisher.publishEvent(new ResumeJobEvent(jobId));

        Job completedJob = awaitJobStatus(jobId, Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(Objects.requireNonNull(completedJob.getOutputs()));

        assertThat(outputs.get("answer")).isEqualTo(FINAL_ANSWER);

        List<Prompt> prompts = scriptedChatModel.getPrompts();

        assertThat(prompts).hasSize(2);
        assertThat(getApprovalToolResponse(prompts.get(1))).contains("NO_RESPONSE");
    }

    @Test
    void testExpiredSuspendResumesTheAgentWithNoResponse() {
        Job suspendedJob = jobSyncExecutor.execute(new JobParametersDTO(WORKFLOW_ID, Map.of()), false);

        assertThat(suspendedJob.getStatus()).isEqualTo(Job.Status.STOPPED);

        JobResumeOutcome jobResumeOutcome = jobResumeFacade.resumeExpiredJob(getJobResumeId(suspendedJob));

        assertThat(jobResumeOutcome).isEqualTo(JobResumeOutcome.OK);

        Job completedJob = awaitJobStatus(Objects.requireNonNull(suspendedJob.getId()), Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(Objects.requireNonNull(completedJob.getOutputs()));

        assertThat(outputs.get("answer")).isEqualTo(FINAL_ANSWER);
        assertThat(getApprovalToolResponse(scriptedChatModel.getPrompts()
            .get(1))).contains("NO_RESPONSE");
    }

    @Test
    void testExpiredSuspendDoesNotResumeTheAgentAgainAfterTheApprovalWasAnswered() {
        Job suspendedJob = jobSyncExecutor.execute(new JobParametersDTO(WORKFLOW_ID, Map.of()), false);

        String jobResumeId = getJobResumeId(suspendedJob);

        assertThat(jobResumeFacade.resumeJob(jobResumeId, Map.of("approved", true))).isEqualTo(JobResumeOutcome.OK);

        long jobId = Objects.requireNonNull(suspendedJob.getId());

        awaitJobStatus(jobId, Job.Status.COMPLETED);

        assertThat(jobResumeFacade.resumeExpiredJob(jobResumeId)).isEqualTo(JobResumeOutcome.GONE);

        Job job = jobService.getJob(jobId);

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);
        assertThat(scriptedChatModel.getPrompts()).hasSize(2);
    }

    @Test
    void testResumeWithEmptyDataResumesTheAgentWithNoResponse() {
        Job suspendedJob = jobSyncExecutor.execute(new JobParametersDTO(WORKFLOW_ID, Map.of()), false);

        assertThat(suspendedJob.getStatus()).isEqualTo(Job.Status.STOPPED);

        JobResumeOutcome jobResumeOutcome = jobResumeFacade.resumeJob(getJobResumeId(suspendedJob), Map.of());

        assertThat(jobResumeOutcome).isEqualTo(JobResumeOutcome.OK);

        awaitJobStatus(Objects.requireNonNull(suspendedJob.getId()), Job.Status.COMPLETED);

        List<Prompt> prompts = scriptedChatModel.getPrompts();

        assertThat(prompts).hasSize(2);
        assertThat(getApprovalToolResponse(prompts.get(1))).contains("NO_RESPONSE");
    }

    @Test
    void testStreamingResumeOfAnApprovalIsNotAllowed() {
        Job suspendedJob = jobSyncExecutor.execute(new JobParametersDTO(WORKFLOW_ID, Map.of()), false);

        JobResumeOutcome jobStreamingResumeOutcome = jobResumeFacade.resumeJobStreaming(
            getJobResumeId(suspendedJob), Map.of("approved", true), jobId -> {});

        assertThat(jobStreamingResumeOutcome).isEqualTo(JobResumeOutcome.STREAMING_NOT_ALLOWED);
        assertThat(jobService.getJob(Objects.requireNonNull(suspendedJob.getId()))
            .getStatus()).isEqualTo(Job.Status.STOPPED);
    }

    @Test
    void testStreamingResumeContinuesTheAgentAndTheResumeIdCannotBeUsedAgain() {
        Job suspendedJob = jobSyncExecutor.execute(new JobParametersDTO(QUESTION_WORKFLOW_ID, Map.of()), false);

        assertThat(suspendedJob.getStatus()).isEqualTo(Job.Status.STOPPED);
        assertThat(suspendedJob.getMetadata(MetadataConstants.STREAMING_RESUME)).isEqualTo(true);

        String jobResumeId = getJobResumeId(suspendedJob);
        List<Long> registeredJobIds = new CopyOnWriteArrayList<>();

        JobResumeOutcome jobStreamingResumeOutcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId, Map.of("message", "Blue"), registeredJobIds::add);

        assertThat(jobStreamingResumeOutcome).isEqualTo(JobResumeOutcome.OK);
        assertThat(registeredJobIds).containsExactly(suspendedJob.getId());

        Job completedJob = awaitJobStatus(Objects.requireNonNull(suspendedJob.getId()), Job.Status.COMPLETED);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(Objects.requireNonNull(completedJob.getOutputs()));

        assertThat(outputs.get("answer")).isEqualTo(FINAL_ANSWER);
        assertThat(getApprovalToolResponse(scriptedChatModel.getPrompts()
            .get(1))).contains("Blue");

        JobResumeOutcome repeatedJobResumeOutcome = jobResumeFacade.resumeJobStreaming(
            jobResumeId, Map.of("message", "Red"), registeredJobIds::add);

        assertThat(repeatedJobResumeOutcome).isEqualTo(JobResumeOutcome.GONE);
        assertThat(registeredJobIds).hasSize(1);
        assertThat(scriptedChatModel.getPrompts()).hasSize(2);
    }

    @Test
    void testRepeatedResumeOfAnAnsweredApprovalIsGoneWhileTheJobRuns() {
        Job suspendedJob = jobSyncExecutor.execute(new JobParametersDTO(WORKFLOW_ID, Map.of()), false);

        String jobResumeId = getJobResumeId(suspendedJob);

        assertThat(jobResumeFacade.resumeJob(jobResumeId, Map.of("approved", true))).isEqualTo(JobResumeOutcome.OK);
        assertThat(jobResumeFacade.resumeJob(jobResumeId, Map.of("approved", false)))
            .isEqualTo(JobResumeOutcome.GONE);

        awaitJobStatus(Objects.requireNonNull(suspendedJob.getId()), Job.Status.COMPLETED);

        assertThat(jobResumeFacade.resumeJob(jobResumeId, Map.of("approved", false)))
            .isEqualTo(JobResumeOutcome.GONE);

        List<Prompt> prompts = scriptedChatModel.getPrompts();

        assertThat(prompts).hasSize(2);
        assertThat(getApprovalToolResponse(prompts.get(1))).isEqualTo("{\"approved\":true}");
    }

    private Job awaitJobStatus(long jobId, Job.Status status) {
        Instant deadline = Instant.now()
            .plus(Duration.ofSeconds(20));

        while (Instant.now()
            .isBefore(deadline)) {

            Job job = jobService.getJob(jobId);

            if (job.getStatus() == status && (status != Job.Status.COMPLETED || job.getOutputs() != null)) {
                return job;
            }

            assertThat(job.getStatus()).isNotEqualTo(Job.Status.FAILED);

            try {
                Thread.sleep(50);
            } catch (InterruptedException interruptedException) {
                Thread currentThread = Thread.currentThread();

                currentThread.interrupt();

                throw new IllegalStateException(interruptedException);
            }
        }

        Job job = jobService.getJob(jobId);

        throw new AssertionError(
            "Job " + jobId + " did not reach status " + status + "; status=" + job.getStatus() + ", metadata=" +
                job.getMetadata() + ", taskExecutions=" + taskExecutionService.getJobTaskExecutions(jobId)
                    .stream()
                    .map(taskExecution -> taskExecution.getStatus() + "/" + taskExecution.getMetadata())
                    .toList()
                +
                ", modelCalls=" + scriptedChatModel.getPrompts()
                    .size());
    }

    private void clearTransientStateLikeADatabaseReload(long taskExecutionId) {
        TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

        taskExecution.setHandled(false);

        taskExecutionService.update(taskExecution);
    }

    private TaskExecution getAgentTaskExecution(Job job) {
        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(
            Objects.requireNonNull(job.getId()));

        return taskExecutions.getFirst();
    }

    private static String getApprovalToolResponse(Prompt prompt) {
        List<Message> instructions = prompt.getInstructions();

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) instructions.getLast();

        List<ToolResponseMessage.ToolResponse> toolResponses = toolResponseMessage.getResponses();

        ToolResponseMessage.ToolResponse toolResponse = toolResponses.getFirst();

        assertThat(toolResponse.name()).isEqualTo(APPROVAL_TOOL_NAME);

        return toolResponse.responseData();
    }

    private static String getJobResumeId(Job job) {
        String jobResumeId = (String) job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        assertThat(jobResumeId).isNotNull();
        assertThat(JobResumeId.parse(jobResumeId)
            .getJobId()).isEqualTo(job.getId());

        return jobResumeId;
    }

    private static ApplicationEventPublisher createEventPublisher(AsyncMessageBroker asyncMessageBroker) {
        return event -> {
            MessageEvent<?> messageEvent = (MessageEvent<?>) event;

            messageEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            asyncMessageBroker.send(messageEvent.getRoute(), messageEvent);
        };
    }

    private static class ModelConnectionTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

        @Override
        public TaskExecution process(TaskExecution taskExecution) {
            taskExecution.putMetadata(MetadataConstants.CONNECTION_IDS, Map.of("model_1", MODEL_CONNECTION_ID));
            taskExecution.putMetadata(MetadataConstants.TYPE, PlatformType.AUTOMATION);

            return taskExecution;
        }

        @Override
        public boolean canProcess(TaskExecution taskExecution) {
            return true;
        }
    }

    private static class InMemoryTaskStateService implements TaskStateService {

        private final Map<String, Object> values = new ConcurrentHashMap<>();

        @Override
        public void delete(JobResumeId jobResumeId) {
            values.remove(jobResumeId.toString());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> fetchValue(JobResumeId jobResumeId) {
            return Optional.ofNullable((T) values.get(jobResumeId.toString()));
        }

        @Override
        public void save(JobResumeId jobResumeId, Object value) {
            values.put(jobResumeId.toString(), value);
        }
    }

    static class ScriptedChatModel implements ChatModel {

        private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            List<Message> instructions = prompt.getInstructions();

            if (instructions.getLast() instanceof ToolResponseMessage) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage(FINAL_ANSWER))));
            }

            AssistantMessage assistantMessage = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", APPROVAL_TOOL_NAME, "{}")))
                .build();

            return new ChatResponse(List.of(new Generation(assistantMessage)));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        @Override
        public ToolCallingChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }

        List<Prompt> getPrompts() {
            return List.copyOf(prompts);
        }

        void reset() {
            prompts.clear();
        }
    }

    @ComponentScan({
        "com.bytechef.component.ai.agent.action", "com.bytechef.component.ai.agent.facade",
        "com.bytechef.component.ai.agent.task", "com.bytechef.component.approval"
    })
    @TestConfiguration
    static class AiAgentToolSuspensionIntTestConfiguration {

        @Bean
        AiAgentComponentHandler aiAgentComponentHandler(
            AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
            ToolCallingManager toolCallingManager) {

            return new AiAgentComponentHandler(aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);
        }

        @Bean
        Evaluator evaluator() {
            return SpelEvaluator.create();
        }

        @Bean
        ScriptedChatModel scriptedChatModel() {
            return new ScriptedChatModel();
        }

        @Bean
        ComponentHandler testModelComponentHandler(ScriptedChatModel scriptedChatModel) {
            ComponentDefinition componentDefinition = component("testModel")
                .title("Test Model")
                .clusterElements(
                    ComponentDsl.<ModelFunction>clusterElement("model")
                        .title("Model")
                        .type(ModelFunction.MODEL)
                        .object(
                            () -> (
                                inputParameters, connectionParameters,
                                responseFormatRequired) -> scriptedChatModel));

            return () -> componentDefinition;
        }

        @Bean
        ComponentHandler testQuestionComponentHandler() {
            ComponentDefinition componentDefinition = component("testQuestion")
                .title("Test Question")
                .clusterElements(
                    ComponentDsl.<MultipleConnectionsToolFunction>clusterElement("askQuestion")
                        .title("Ask Question")
                        .description("Asks the user a question and waits for the answer.")
                        .type(BaseToolFunction.TOOLS)
                        .object(() -> (
                            inputParameters, connectionParameters, extensions, componentConnections, context) -> {

                            ClusterElementContextAware clusterElementContextAware =
                                (ClusterElementContextAware) context;

                            ActionContext actionContext = clusterElementContextAware.toActionContext(
                                "testQuestion", 1, "askQuestion", null);

                            actionContext.suspend(
                                new ActionContext.Suspend(
                                    Map.of(MetadataConstants.STREAMING_RESUME, true),
                                    Instant.now()
                                        .plus(Duration.ofDays(1))));

                            return ToolSuspension.suspendedToolResult(actionContext);
                        }));

            return () -> componentDefinition;
        }

        @Bean
        @Primary
        Tracer noopTracer() {
            return Tracer.NOOP;
        }

        @Bean
        ToolCallingManager toolCallingManager() {
            return DefaultToolCallingManager.builder()
                .toolCallbackResolver(new DelegatingToolCallbackResolver(List.of()))
                .build();
        }
    }
}
