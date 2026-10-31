/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.coordinator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.repository.resource.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.ErrorEventListener;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcContextRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.workflow.repository.classpath.enabled=true"
    })
@Import({
    JacksonConfiguration.class, PostgreSQLContainerConfiguration.class
})
public class TaskCoordinatorIntTest {

    private static final TaskFileStorage TASK_FILE_STORAGE = new TaskFileStorageImpl(new Base64FileStorageService());

    private final Evaluator evaluator = SpelEvaluator.create();

    @Autowired
    private ContextService contextService;

    @Autowired
    private Environment environment;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private WorkflowService workflowService;

    @Test
    public void testPerformWorkflowJson() {
        Job completedJob = executeWorkflow("aGVsbG8x");

        Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testPerformWorkflowYaml() {
        Job completedJob = executeWorkflow("aGVsbG8y");

        Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());
    }

    @Test
    public void testPerformWorkflowWithDisabledTask() {
        Job completedJob = executeWorkflow("aGVsbG8z");

        Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(
            Objects.requireNonNull(completedJob.getId()));

        Assertions.assertEquals(
            List.of("firstNumber", "lastNumber"),
            taskExecutions.stream()
                .map(TaskExecution::getName)
                .toList());

        Map<String, ?> outputs = TASK_FILE_STORAGE.readJobOutputs(
            Objects.requireNonNull(completedJob.getOutputs()));

        Assertions.assertTrue(outputs.containsKey("skippedValue"));
        Assertions.assertNull(outputs.get("skippedValue"));
        Assertions.assertTrue(outputs.containsKey("skippedField"));
        Assertions.assertNull(outputs.get("skippedField"));
    }

    @Test
    public void testPerformWorkflowWithFirstTaskDisabled() {
        Job completedJob = executeWorkflow("aGVsbG81");

        Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(
            Objects.requireNonNull(completedJob.getId()));

        Assertions.assertEquals(
            List.of("secondNumber", "thirdNumber"),
            taskExecutions.stream()
                .map(TaskExecution::getName)
                .toList());

        Map<String, ?> outputs = TASK_FILE_STORAGE.readJobOutputs(
            Objects.requireNonNull(completedJob.getOutputs()));

        Assertions.assertTrue(outputs.containsKey("skippedValue"));
        Assertions.assertNull(outputs.get("skippedValue"));
    }

    @Test
    public void testPerformWorkflowWithAllTasksDisabled() {
        Job completedJob = executeWorkflow("aGVsbG80");

        Assertions.assertEquals(Job.Status.COMPLETED, completedJob.getStatus());
        Assertions.assertEquals(-1, completedJob.getCurrentTask());
        Assertions.assertNotNull(completedJob.getEndDate());

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(
            Objects.requireNonNull(completedJob.getId()));

        Assertions.assertTrue(taskExecutions.isEmpty());

        Map<String, ?> outputs = TASK_FILE_STORAGE.readJobOutputs(
            Objects.requireNonNull(completedJob.getOutputs()));

        Assertions.assertTrue(outputs.containsKey("skippedValue"));
        Assertions.assertNull(outputs.get("skippedValue"));
    }

    @Test
    public void testCompletionOfCancelledTaskExecutionIsDropped() {
        long jobId = saveJob(Job.Status.STARTED, "aGVsbG8x", 0);

        TaskExecution cancelledTaskExecution = saveTaskExecution(jobId, null, TaskExecution.Status.CANCELLED);

        TaskCompletionHandler taskCompletionHandler = mock(TaskCompletionHandler.class);

        TaskCoordinator taskCoordinator = createTaskCoordinator(
            List.of(), List.of(), mock(ApplicationEventPublisher.class), mock(JobExecutor.class),
            taskCompletionHandler, newTaskDispatcher());

        TaskExecution completedTaskExecution = taskExecutionService.getTaskExecution(
            Objects.requireNonNull(cancelledTaskExecution.getId()));

        completedTaskExecution.setStatus(TaskExecution.Status.STARTED);

        taskCoordinator.onTaskExecutionCompleteEvent(new TaskExecutionCompleteEvent(completedTaskExecution));

        verify(taskCompletionHandler, never()).handle(any());

        assertThat(getTaskExecutionStatus(cancelledTaskExecution)).isEqualTo(TaskExecution.Status.CANCELLED);
    }

    @Test
    public void testCompletionOfStartedTaskExecutionIsHandled() {
        long jobId = saveJob(Job.Status.STARTED, "aGVsbG8x", 0);

        TaskExecution startedTaskExecution = saveTaskExecution(jobId, null, TaskExecution.Status.STARTED);

        TaskCompletionHandler taskCompletionHandler = mock(TaskCompletionHandler.class);

        TaskCoordinator taskCoordinator = createTaskCoordinator(
            List.of(), List.of(), mock(ApplicationEventPublisher.class), mock(JobExecutor.class),
            taskCompletionHandler, newTaskDispatcher());

        taskCoordinator.onTaskExecutionCompleteEvent(new TaskExecutionCompleteEvent(startedTaskExecution));

        verify(taskCompletionHandler).handle(startedTaskExecution);

        assertThat(getTaskExecutionStatus(startedTaskExecution)).isEqualTo(TaskExecution.Status.COMPLETED);
    }

    @Test
    public void testApplicationEventReachesRemainingListenersWhenOneListenerThrows() {
        ApplicationEventListener failingApplicationEventListener = mock(ApplicationEventListener.class);
        ApplicationEventListener applicationEventListener = mock(ApplicationEventListener.class);
        JobStatusApplicationEvent jobStatusApplicationEvent = new JobStatusApplicationEvent(4567L, Job.Status.FAILED);

        doThrow(new IllegalStateException("No notification sender available"))
            .when(failingApplicationEventListener)
            .onApplicationEvent(jobStatusApplicationEvent);

        TaskCoordinator taskCoordinator = createTaskCoordinator(
            List.of(failingApplicationEventListener, applicationEventListener), List.of(),
            mock(ApplicationEventPublisher.class), mock(JobExecutor.class), mock(TaskCompletionHandler.class),
            newTaskDispatcher());

        taskCoordinator.onApplicationEvent(jobStatusApplicationEvent);

        verify(failingApplicationEventListener).onApplicationEvent(jobStatusApplicationEvent);
        verify(applicationEventListener).onApplicationEvent(jobStatusApplicationEvent);
    }

    @Test
    public void testErrorEventReachesRemainingListenersWhenOneListenerThrows() {
        ErrorEventListener failingErrorEventListener = mock(ErrorEventListener.class);
        ErrorEventListener errorEventListener = mock(ErrorEventListener.class);

        long jobId = saveJob(Job.Status.STARTED, "aGVsbG8x", 0);

        TaskExecutionErrorEvent taskExecutionErrorEvent = new TaskExecutionErrorEvent(
            saveTaskExecution(jobId, null, TaskExecution.Status.FAILED));

        doThrow(new IllegalStateException("Error listener failed"))
            .when(failingErrorEventListener)
            .onErrorEvent(taskExecutionErrorEvent);

        TaskCoordinator taskCoordinator = createTaskCoordinator(
            List.of(), List.of(failingErrorEventListener, errorEventListener), mock(ApplicationEventPublisher.class),
            mock(JobExecutor.class), mock(TaskCompletionHandler.class), newTaskDispatcher());

        taskCoordinator.onErrorEvent(taskExecutionErrorEvent);

        verify(failingErrorEventListener).onErrorEvent(taskExecutionErrorEvent);
        verify(errorEventListener).onErrorEvent(taskExecutionErrorEvent);
    }

    @Test
    public void testStartOfJobCompletedDuringExecutionDoesNotPublishStartedStatus() {
        long jobId = saveJob(Job.Status.CREATED, "aGVsbG8x", 0);

        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        JobExecutor jobExecutor = mock(JobExecutor.class);

        doAnswer(invocation -> {
            Job job = invocation.getArgument(0);

            job.setStatus(Job.Status.COMPLETED);

            jobService.update(job);

            return null;
        }).when(jobExecutor)
            .execute(any(Job.class));

        TaskCoordinator taskCoordinator = createTaskCoordinator(
            List.of(), List.of(), eventPublisher, jobExecutor, mock(TaskCompletionHandler.class),
            newTaskDispatcher());

        taskCoordinator.onStartJobEvent(new StartJobEvent(jobId));

        verify(eventPublisher, never()).publishEvent(any(JobStatusApplicationEvent.class));

        assertThat(getJob(jobId).getStatus()).isEqualTo(Job.Status.COMPLETED);
    }

    @Test
    public void testStartOfJobStillRunningAfterExecutionPublishesStartedStatus() {
        long jobId = saveJob(Job.Status.CREATED, "aGVsbG8x", 0);

        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        TaskCoordinator taskCoordinator = createTaskCoordinator(
            List.of(), List.of(), eventPublisher, mock(JobExecutor.class), mock(TaskCompletionHandler.class),
            newTaskDispatcher());

        taskCoordinator.onStartJobEvent(new StartJobEvent(jobId));

        ArgumentCaptor<JobStatusApplicationEvent> jobStatusApplicationEventArgumentCaptor = ArgumentCaptor.forClass(
            JobStatusApplicationEvent.class);

        verify(eventPublisher).publishEvent(jobStatusApplicationEventArgumentCaptor.capture());

        JobStatusApplicationEvent jobStatusApplicationEvent = jobStatusApplicationEventArgumentCaptor.getValue();

        assertThat(jobStatusApplicationEvent.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(getJob(jobId).getStatus()).isEqualTo(Job.Status.STARTED);
    }

    @Test
    public void testResumeOfFailedJobDispatchesFailedTaskWithCompletedTaskOutputs() {
        Job job = new Job();

        job.setCurrentTask(1);
        job.setEndDate(Instant.now());
        job.setStatus(Job.Status.FAILED);
        job.setWorkflowId(EncodingUtils.base64EncodeToString("resume-failed"));

        job = jobRepository.save(job);

        long jobId = Objects.requireNonNull(job.getId());

        contextService.push(
            jobId, Context.Classname.JOB,
            TASK_FILE_STORAGE.storeContextValue(jobId, Context.Classname.JOB, Map.of("task_1", "task 1 output")));

        TaskDispatcher<? super Task> taskDispatcher = newTaskDispatcher();

        TaskCoordinator taskCoordinator = createTaskCoordinator(
            List.of(), List.of(), mock(ApplicationEventPublisher.class), newJobExecutor(taskDispatcher),
            mock(TaskCompletionHandler.class), taskDispatcher);

        taskCoordinator.onResumeJobEvent(new ResumeJobEvent(jobId));

        ArgumentCaptor<TaskExecution> taskExecutionArgumentCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskDispatcher).dispatch(taskExecutionArgumentCaptor.capture());

        TaskExecution dispatchedTaskExecution = taskExecutionArgumentCaptor.getValue();

        assertThat(dispatchedTaskExecution.getName()).isEqualTo("task_2");

        Map<String, ?> parameters = dispatchedTaskExecution.getParameters();

        assertThat(parameters).extractingByKey("value")
            .isEqualTo("task 1 output");
        assertThat(taskExecutionService.getJobTaskExecutions(jobId))
            .extracting(TaskExecution::getId)
            .containsExactly(dispatchedTaskExecution.getId());

        Job resumedJob = getJob(jobId);

        assertThat(resumedJob.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(resumedJob.getEndDate()).isNull();
        assertThat(resumedJob.getCurrentTask()).isEqualTo(1);
    }

    @Test
    public void testResumeOfAlreadyStartedJobDispatchesCurrentTaskWithoutTransitioningAgain() {
        long jobId = saveJob(Job.Status.STARTED, EncodingUtils.base64EncodeToString("resume-started"), 1);

        contextService.push(
            jobId, Context.Classname.JOB, TASK_FILE_STORAGE.storeContextValue(jobId, Context.Classname.JOB, Map.of()));

        Job originalJob = getJob(jobId);

        TaskDispatcher<? super Task> taskDispatcher = newTaskDispatcher();

        TaskCoordinator taskCoordinator = createTaskCoordinator(
            List.of(), List.of(), mock(ApplicationEventPublisher.class), newJobExecutor(taskDispatcher),
            mock(TaskCompletionHandler.class), taskDispatcher);

        taskCoordinator.onResumeJobEvent(ResumeJobEvent.ofStartedJob(jobId));

        ArgumentCaptor<TaskExecution> taskExecutionArgumentCaptor = ArgumentCaptor.forClass(TaskExecution.class);

        verify(taskDispatcher).dispatch(taskExecutionArgumentCaptor.capture());

        TaskExecution dispatchedTaskExecution = taskExecutionArgumentCaptor.getValue();

        assertThat(dispatchedTaskExecution.getName()).isEqualTo("task_2");

        Job persistedJob = getJob(jobId);

        assertThat(persistedJob.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(persistedJob.getVersion()).isEqualTo(originalJob.getVersion());
        assertThat(persistedJob.getLastModifiedDate()).isEqualTo(originalJob.getLastModifiedDate());
    }

    @Test
    public void testStopJobCancelsEveryUnfinishedTaskExecution() {
        long jobId = saveJob(Job.Status.STARTED, "aGVsbG8x", 0);

        TaskExecution outerLoopTaskExecution = saveTaskExecution(jobId, null, TaskExecution.Status.STARTED);
        TaskExecution completedTaskExecution = saveTaskExecution(jobId, null, TaskExecution.Status.COMPLETED);
        TaskExecution innerLoopTaskExecution = saveTaskExecution(
            jobId, outerLoopTaskExecution.getId(), TaskExecution.Status.STARTED);
        TaskExecution innerTaskExecution = saveTaskExecution(
            jobId, innerLoopTaskExecution.getId(), TaskExecution.Status.CREATED);

        List<TaskExecution> unfinishedTaskExecutions = List.of(
            outerLoopTaskExecution, innerLoopTaskExecution, innerTaskExecution);
        List<List<TaskExecution.Status>> statusesAtPublish = new CopyOnWriteArrayList<>();

        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        doAnswer(invocation -> {
            statusesAtPublish.add(
                unfinishedTaskExecutions.stream()
                    .map(this::getTaskExecutionStatus)
                    .toList());

            return null;
        }).when(eventPublisher)
            .publishEvent(isA(JobStatusApplicationEvent.class));

        TaskDispatcher<? super Task> taskDispatcher = newTaskDispatcher();

        TaskCoordinator taskCoordinator = createTaskCoordinator(
            List.of(), List.of(), eventPublisher, mock(JobExecutor.class), mock(TaskCompletionHandler.class),
            taskDispatcher);

        taskCoordinator.onStopJobEvent(new StopJobEvent(jobId));

        assertThat(statusesAtPublish)
            .as("every unfinished task execution must be cancelled before the job status is published")
            .containsExactly(
                List.of(
                    TaskExecution.Status.CANCELLED, TaskExecution.Status.CANCELLED, TaskExecution.Status.CANCELLED));
        assertThat(getTaskExecutionStatus(completedTaskExecution)).isEqualTo(TaskExecution.Status.COMPLETED);
        assertThat(getJob(jobId).getStatus()).isEqualTo(Job.Status.STOPPED);

        ArgumentCaptor<Task> taskArgumentCaptor = ArgumentCaptor.forClass(Task.class);

        verify(taskDispatcher).dispatch(taskArgumentCaptor.capture());

        assertThat(taskArgumentCaptor.getValue()).isInstanceOf(CancelControlTask.class);
    }

    private TaskCoordinator createTaskCoordinator(
        List<ApplicationEventListener> applicationEventListeners, List<ErrorEventListener> errorEventListeners,
        ApplicationEventPublisher eventPublisher, JobExecutor jobExecutor, TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher) {

        return new TaskCoordinator(
            applicationEventListeners, errorEventListeners, eventPublisher, jobExecutor, jobService,
            taskCompletionHandler, taskDispatcher, taskExecutionService);
    }

    private Job getJob(long jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow();
    }

    private TaskExecution.Status getTaskExecutionStatus(TaskExecution taskExecution) {
        TaskExecution persistedTaskExecution = taskExecutionRepository.findById(
            Objects.requireNonNull(taskExecution.getId()))
            .orElseThrow();

        return persistedTaskExecution.getStatus();
    }

    private JobExecutor newJobExecutor(TaskDispatcher<? super Task> taskDispatcher) {
        return new JobExecutor(
            contextService, evaluator, mock(ApplicationEventPublisher.class), jobService, taskDispatcher,
            taskExecutionService, TASK_FILE_STORAGE, workflowService);
    }

    @SuppressWarnings("unchecked")
    private static TaskDispatcher<? super Task> newTaskDispatcher() {
        return (TaskDispatcher<? super Task>) mock(TaskDispatcher.class);
    }

    private long saveJob(Job.Status status, String workflowId, int currentTask) {
        Job job = new Job();

        job.setCurrentTask(currentTask);
        job.setStatus(status);
        job.setWorkflowId(workflowId);

        job = jobRepository.save(job);

        return Objects.requireNonNull(job.getId());
    }

    private TaskExecution saveTaskExecution(long jobId, Long parentId, TaskExecution.Status status) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .parentId(parentId)
            .status(status)
            .taskNumber(1)
            .workflowTask(new WorkflowTask(Map.of("name", "task1", "type", "noop/v1/noop")))
            .build();

        return taskExecutionRepository.save(taskExecution);
    }

    private Job executeWorkflow(String workflowId) {
        Map<String, TaskHandler<?>> taskHandlerMap = new HashMap<>();

        taskHandlerMap.put("randomHelper/v1/randomInt", taskExecution -> null);

        JobSyncExecutor jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, jobService, -1, new AsyncMessageBroker(environment), List.of(),
            taskExecutionService,
            new TaskExecutor() {
                private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

                @Override
                public void execute(Runnable task) {
                    executor.execute(task);
                }
            }, taskHandlerMap::get, TASK_FILE_STORAGE, -1, workflowService);

        return jobSyncExecutor.execute(
            new JobParametersDTO(workflowId, Collections.singletonMap("yourName", "me")), true);
    }

    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
    })
    @Configuration
    public static class CoordinatorIntTestConfiguration {

        @Bean
        ClassPathResourceWorkflowRepository classPathResourceWorkflowRepository(
            ResourcePatternResolver resourcePatternResolver) {

            return new ClassPathResourceWorkflowRepository("workflows/**/*.{json|yml|yaml}", resourcePatternResolver);
        }

        @Bean
        ContextService contextService(JdbcContextRepository jdbcContextRepository) {
            return new ContextServiceImpl(jdbcContextRepository);
        }

        @Bean
        JobService jobService(JdbcJobRepository jdbcJobRepository) {
            return new JobServiceImpl(jdbcJobRepository);
        }

        @Bean
        TaskExecutionService taskExecutionService(JdbcTaskExecutionRepository jdbcTaskExecutionRepository) {
            return new TaskExecutionServiceImpl(jdbcTaskExecutionRepository);
        }

        @Bean
        WorkflowService workflowService(
            List<WorkflowCrudRepository> workflowCrudRepositories, List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(
                new ConcurrentMapCacheManager(), workflowCrudRepositories, workflowRepositories);
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        public static class CoordinatorIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            public CoordinatorIntTestJdbcConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return Arrays.asList(
                    new ExecutionErrorToStringConverter(objectMapper),
                    new FileEntryToStringConverter(objectMapper),
                    new MapWrapperToStringConverter(objectMapper),
                    new StringToExecutionErrorConverter(objectMapper),
                    new StringToExecutionErrorConverter(objectMapper),
                    new StringToFileEntryConverter(objectMapper),
                    new StringToMapWrapperConverter(objectMapper),
                    new StringToWebhooksConverter(objectMapper),
                    new StringToWorkflowTaskConverter(objectMapper),
                    new WebhooksToStringConverter(objectMapper),
                    new WorkflowTaskToStringConverter(objectMapper));
            }
        }
    }
}
