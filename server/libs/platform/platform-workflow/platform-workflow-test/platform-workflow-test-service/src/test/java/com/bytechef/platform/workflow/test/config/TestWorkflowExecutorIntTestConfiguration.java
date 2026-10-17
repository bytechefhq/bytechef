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

package com.bytechef.platform.workflow.test.config;

import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ActionDefinitionServiceImpl;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionServiceImpl;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionServiceImpl;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionServiceImpl;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.cache.WorkflowCacheManagerImpl;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacadeImpl;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationConnectionRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputServiceImpl;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationServiceImpl;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionRegistry;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionServiceImpl;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableAutoConfiguration
@EnableCaching
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class,
    TestWorkflowExecutorIntTestConfiguration.TestWorkflowExecutorIntTestJdbcConfiguration.class,
    WorkflowTestConfiguration.class
})
public class TestWorkflowExecutorIntTestConfiguration {

    public static final String COMPONENT_NAME = "testComponent";
    public static final String COMPONENT_TITLE = "Test Component";
    public static final String EVALUATION_PROBE = "evaluationProbe";
    public static final String EVALUATION_PROBE_EXPRESSION = "=" + EVALUATION_PROBE + "()";
    public static final String FAILED_TASK_MESSAGE = "Task failed on purpose";
    public static final String GATE = "gate";
    public static final String STREAMED_CHUNK = "streamed";

    private static final ComponentDefinition COMPONENT_DEFINITION = ComponentDsl.component(COMPONENT_NAME)
        .title(COMPONENT_TITLE)
        .actions(
            ComponentDsl.action("block")
                .title("Block"),
            ComponentDsl.action("echo")
                .title("Echo"),
            ComponentDsl.action("fail")
                .title("Fail"),
            ComponentDsl.action("stream")
                .title("Stream"))
        .triggers(
            ComponentDsl.trigger("start")
                .title("Start")
                .type(TriggerType.STATIC_WEBHOOK));

    @Bean
    ActionDefinitionFacade actionDefinitionFacade() {
        return mock(ActionDefinitionFacade.class);
    }

    @Bean
    ActionDefinitionService actionDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory) {

        return new ActionDefinitionServiceImpl(componentDefinitionRegistry, contextFactory);
    }

    @Bean
    ClusterElementDefinitionFacade clusterElementDefinitionFacade() {
        return mock(ClusterElementDefinitionFacade.class);
    }

    @Bean
    ClusterElementDefinitionService clusterElementDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory) {

        return new ClusterElementDefinitionServiceImpl(
            componentDefinitionRegistry, contextFactory, mock(ObjectProvider.class));
    }

    @Bean
    ComponentDefinitionRegistry componentDefinitionRegistry(
        ApplicationProperties applicationProperties, List<ComponentHandler> componentHandlers) {

        return new ComponentDefinitionRegistry(applicationProperties, componentHandlers, List::of, List.of());
    }

    @Bean
    ComponentDefinitionService componentDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory) {

        return new ComponentDefinitionServiceImpl(List.of(), componentDefinitionRegistry, contextFactory);
    }

    @Bean
    ContextFactory contextFactory() {
        return mock(ContextFactory.class);
    }

    @Bean
    EvaluationProbe evaluationProbe() {
        return new EvaluationProbe();
    }

    @Bean
    Evaluator evaluator(EvaluationProbe evaluationProbe) {
        return SpelEvaluator.builder()
            .methodExecutor(EVALUATION_PROBE, evaluationProbe)
            .build();
    }

    @Bean(name = "syncWorkerExecutor")
    TaskExecutor syncWorkerExecutor() {
        return new SimpleAsyncTaskExecutor("sync-worker-");
    }

    @Bean
    SubflowResolver subflowResolver() {
        return (workflowUuid, triggerName, editorEnvironment) -> {
            throw new UnsupportedOperationException("Subflows are not part of these tests");
        };
    }

    @Bean
    TaskDispatcherDefinitionService taskDispatcherDefinitionService(
        ObjectProvider<TaskDispatcherDefinitionFactory> taskDispatcherDefinitionFactories) {

        return new TaskDispatcherDefinitionServiceImpl(
            new TaskDispatcherDefinitionRegistry(
                taskDispatcherDefinitionFactories.orderedStream()
                    .toList()));
    }

    @Bean
    TaskGates taskGates() {
        return new TaskGates();
    }

    @Bean
    TaskHandlerRegistry taskHandlerRegistry(TaskGates taskGates) {
        Map<String, TaskHandler<?>> taskHandlers = Map.of(
            COMPONENT_NAME + "/v1/block", taskExecution -> {
                TaskGate taskGate = taskGates.get(MapUtils.getRequiredString(taskExecution.getParameters(), GATE));

                taskGate.pass();

                return getParameters(taskExecution);
            },
            COMPONENT_NAME + "/v1/echo", TestWorkflowExecutorIntTestConfiguration::getParameters,
            COMPONENT_NAME + "/v1/fail", taskExecution -> {
                throw new IllegalStateException(FAILED_TASK_MESSAGE);
            },
            COMPONENT_NAME + "/v1/stream", taskExecution -> (SseEmitterHandler) sseEmitter -> {
                sseEmitter.send(Map.of("chunk", STREAMED_CHUNK));
                sseEmitter.complete();
            });

        return type -> {
            TaskHandler<?> taskHandler = taskHandlers.get(type);

            if (taskHandler == null) {
                throw new IllegalArgumentException("No task handler registered for type " + type);
            }

            return taskHandler;
        };
    }

    @Bean
    TempFileStorage tempFileStorage() {
        return mock(TempFileStorage.class);
    }

    @Bean
    ComponentHandler testComponentHandler() {
        return () -> COMPONENT_DEFINITION;
    }

    @Bean
    TriggerDefinitionFacade triggerDefinitionFacade() {
        return mock(TriggerDefinitionFacade.class);
    }

    @Bean
    TriggerDefinitionService triggerDefinitionService(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory,
        ApplicationEventPublisher applicationEventPublisher) {

        return new TriggerDefinitionServiceImpl(componentDefinitionRegistry, contextFactory, applicationEventPublisher);
    }

    @Bean
    WorkflowCacheManager workflowCacheManager(CacheManager cacheManager, WorkflowService workflowService) {
        return new WorkflowCacheManagerImpl(cacheManager, workflowService);
    }

    @Bean
    WorkflowNodeOutputFacade workflowNodeOutputFacade(
        ActionDefinitionFacade actionDefinitionFacade, ActionDefinitionService actionDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        WorkflowCacheManager workflowCacheManager, WorkflowService workflowService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        return new WorkflowNodeOutputFacadeImpl(
            actionDefinitionFacade, actionDefinitionService, clusterElementDefinitionFacade,
            clusterElementDefinitionService, evaluator, taskDispatcherDefinitionService, triggerDefinitionFacade,
            triggerDefinitionService, workflowCacheManager, workflowService, workflowNodeTestOutputService,
            workflowTestConfigurationService);
    }

    @Bean
    WorkflowNodeTestOutputService workflowNodeTestOutputService(
        CacheManager cacheManager, WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository) {

        return new WorkflowNodeTestOutputServiceImpl(cacheManager, workflowNodeTestOutputRepository);
    }

    @Bean
    WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }

    @Bean
    WorkflowTestConfigurationService workflowTestConfigurationService(
        WorkflowTestConfigurationRepository workflowTestConfigurationRepository,
        WorkflowTestConfigurationConnectionRepository workflowTestConfigurationConnectionRepository) {

        return new WorkflowTestConfigurationServiceImpl(
            workflowTestConfigurationRepository, workflowTestConfigurationConnectionRepository);
    }

    private static Map<String, ?> getParameters(TaskExecution taskExecution) {
        return new HashMap<>(taskExecution.getParameters());
    }

    public static final class EvaluationProbe implements MethodExecutor {

        private final AtomicInteger activeCalls = new AtomicInteger();
        private final AtomicInteger armedCalls = new AtomicInteger();

        private volatile CountDownLatch firstCallEnteredLatch = new CountDownLatch(1);
        private volatile CountDownLatch firstCallReleaseLatch = new CountDownLatch(0);
        private volatile Mode mode = Mode.PASS;
        private volatile CountDownLatch overlappingCallLatch = new CountDownLatch(1);

        @Override
        public TypedValue execute(EvaluationContext context, Object target, Object... arguments) {
            Mode currentMode = mode;

            if (currentMode == Mode.FAIL) {
                throw new IllegalArgumentException("Execution failed");
            }

            if (currentMode == Mode.BLOCK_FIRST_CALL) {
                blockFirstCall();
            }

            return new TypedValue(EVALUATION_PROBE_EXPRESSION);
        }

        public boolean awaitFirstCallEntered(long timeout, TimeUnit timeUnit) throws InterruptedException {
            return firstCallEnteredLatch.await(timeout, timeUnit);
        }

        public boolean awaitOverlappingCall(long timeout, TimeUnit timeUnit) throws InterruptedException {
            return overlappingCallLatch.await(timeout, timeUnit);
        }

        public int getArmedCalls() {
            return armedCalls.get();
        }

        public void armBlockingFirstCall() {
            firstCallEnteredLatch = new CountDownLatch(1);
            firstCallReleaseLatch = new CountDownLatch(1);
            overlappingCallLatch = new CountDownLatch(1);

            armedCalls.set(0);

            mode = Mode.BLOCK_FIRST_CALL;
        }

        public void armFailure() {
            mode = Mode.FAIL;
        }

        public void releaseFirstCall() {
            firstCallReleaseLatch.countDown();
        }

        public void reset() {
            mode = Mode.PASS;

            firstCallReleaseLatch.countDown();
        }

        private void blockFirstCall() {
            if (activeCalls.incrementAndGet() > 1) {
                overlappingCallLatch.countDown();
            }

            try {
                if (armedCalls.incrementAndGet() == 1) {
                    firstCallEnteredLatch.countDown();

                    if (!firstCallReleaseLatch.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("The first probed evaluation was never released");
                    }
                }
            } catch (InterruptedException interruptedException) {
                Thread currentThread = Thread.currentThread();

                currentThread.interrupt();
            } finally {
                activeCalls.decrementAndGet();
            }
        }

        private enum Mode {
            BLOCK_FIRST_CALL, FAIL, PASS
        }
    }

    public static final class TaskGate {

        private final CountDownLatch enteredLatch = new CountDownLatch(1);
        private final AtomicBoolean interrupted = new AtomicBoolean();
        private final CountDownLatch releaseLatch = new CountDownLatch(1);

        public boolean awaitEntered(long timeout, TimeUnit timeUnit) throws InterruptedException {
            return enteredLatch.await(timeout, timeUnit);
        }

        public boolean isInterrupted() {
            return interrupted.get();
        }

        public void release() {
            releaseLatch.countDown();
        }

        private void pass() {
            enteredLatch.countDown();

            try {
                if (!releaseLatch.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("The task gate was never released");
                }
            } catch (InterruptedException interruptedException) {
                interrupted.set(true);

                Thread currentThread = Thread.currentThread();

                currentThread.interrupt();

                throw new IllegalStateException("The task gate was interrupted", interruptedException);
            }
        }
    }

    public static final class TaskGates {

        private final Map<String, TaskGate> gates = new ConcurrentHashMap<>();

        public TaskGate get(String name) {
            return gates.computeIfAbsent(name, key -> new TaskGate());
        }

        public void reset() {
            for (TaskGate taskGate : gates.values()) {
                taskGate.release();
            }

            gates.clear();
        }
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class TestWorkflowExecutorIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public TestWorkflowExecutorIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new MapWrapperToStringConverter(objectMapper), new StringToMapWrapperConverter(objectMapper),
                new StringToWorkflowTaskConverter(objectMapper), new WorkflowTaskToStringConverter(objectMapper));
        }
    }
}
