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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.repository.resource.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition.OutputFunction;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ActionDefinitionFacadeImpl;
import com.bytechef.platform.component.service.ActionDefinitionServiceImpl;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationFacade;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationResult;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationResult.Outcome;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import com.bytechef.platform.workflow.worker.task.handler.AbstractTaskHandler;
import com.bytechef.tenant.TenantContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class WorkflowTestConfigurationTest {

    private static final String SAMPLE_VALUE = "sample";
    private static final String TRAP = "trap";

    private final Map<String, Map<String, Object>> handledParameters = new ConcurrentHashMap<>();
    private final Queue<String> handledTasks = new ConcurrentLinkedQueue<>();
    private final AtomicInteger outputFunctionCalls = new AtomicInteger();
    private final AtomicInteger performCalls = new AtomicInteger();

    private ActionDefinitionFacade actionDefinitionFacade;
    private ConnectionService connectionService;
    private ObjectMapper objectMapper;
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        objectMapper = JsonMapper.builder()
            .build();

        ConvertUtils.setObjectMapper(objectMapper);
        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);

        TenantContext.setCurrentTenantId("public");

        ComponentDefinitionRegistry componentDefinitionRegistry = new ComponentDefinitionRegistry(
            new ApplicationProperties(), List.of(createTrapComponentHandler()), List::of, List.of());

        connectionService = mock(ConnectionService.class);

        actionDefinitionFacade = spy(
            new ActionDefinitionFacadeImpl(
                connectionService,
                new ActionDefinitionServiceImpl(componentDefinitionRegistry, mock(ContextFactory.class))));

        workflowService = new WorkflowServiceImpl(
            new ConcurrentMapCacheManager(), List.of(),
            List.of(
                new ClassPathResourceWorkflowRepository(
                    "workflows/**/*.{json|yml|yaml}", new PathMatchingResourcePatternResolver())));
    }

    @Test
    void testWorkflowSimulationFacadeDryRunsEveryTaskOfEveryDispatcherWithoutPerforming() {
        WorkflowSimulationFacade workflowSimulationFacade = createWorkflowSimulationFacade();

        WorkflowSimulationResult workflowSimulationResult = workflowSimulationFacade.simulate(
            EncodingUtils.base64EncodeToString("simulationAllDispatchers"), Map.of());

        assertThat(performCalls).hasValue(0);
        assertThat(outputFunctionCalls).hasValue(0);
        assertThat(workflowSimulationResult.outcome()).isEqualTo(Outcome.COMPLETED);
        assertThat(new ArrayList<>(handledTasks)).containsExactlyInAnyOrder(
            "top_1:true", "branchCase1:true", "branchCase2:true", "branchDefault:true", "conditionTrue:true",
            "conditionFalse:true", "mapItem:true", "mapItem:true", "parallelA:true", "parallelB:true", "forkA:true",
            "forkB:true", "onErrorBranch:true", "childTask:true", "agent_1:true", "dynamicOutput_1:true",
            "final_1:true");
        assertThat(handledParameters.get("final_1")).containsEntry("value", SAMPLE_VALUE);

        verify(actionDefinitionFacade, never()).executePerform(
            any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(),
            any(), any(), any());
        verifyNoInteractions(connectionService);
    }

    @Test
    void testTrapComponentPerformsWhenTheDryRunFlagIsAbsent() {
        TaskHandler<?> taskHandler = createTaskHandlerRegistry().getTaskHandler(TRAP + "/v1/perform");

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(1L)
            .workflowTask(new WorkflowTask(Map.of("name", "top_1", "type", TRAP + "/v1/perform")))
            .build();

        assertThatThrownBy(() -> taskHandler.handle(taskExecution)).isInstanceOf(TaskExecutionException.class);
        assertThat(performCalls).hasValue(1);
    }

    private ComponentHandler createTrapComponentHandler() {
        PerformFunction trapPerformFunction = (inputParameters, connectionParameters, context) -> {
            performCalls.incrementAndGet();

            throw new IllegalStateException("The real perform function was called");
        };

        OutputFunction trapOutputFunction = (inputParameters, connectionParameters, context) -> {
            outputFunctionCalls.incrementAndGet();

            throw new IllegalStateException("The dynamic output function was called");
        };

        ComponentDefinition componentDefinition = ComponentDsl.component(TRAP)
            .title("Trap")
            .actions(
                ComponentDsl.action("agent")
                    .title("Agent")
                    .perform(trapPerformFunction),
                ComponentDsl.action("dynamicOutput")
                    .title("Dynamic Output")
                    .output(trapOutputFunction)
                    .perform(trapPerformFunction),
                ComponentDsl.action("perform")
                    .title("Perform")
                    .output(ComponentDsl.sampleOutput(Map.of("value", SAMPLE_VALUE)))
                    .perform(trapPerformFunction));

        return () -> componentDefinition;
    }

    private TaskHandlerRegistry createTaskHandlerRegistry() {
        return type -> {
            if (type.equals("unsimulatable/v1/run")) {
                return taskExecution -> {
                    performCalls.incrementAndGet();

                    throw new IllegalStateException("A task handler outside the component perform path was called");
                };
            }

            String[] typeItems = type.split("/");

            if (typeItems.length != 3 || !typeItems[0].equals(TRAP)) {
                throw new IllegalArgumentException("No task handler registered for type " + type);
            }

            return new RecordingTaskHandler(typeItems[2]);
        };
    }

    private WorkflowSimulationFacade createWorkflowSimulationFacade() {
        SubflowResolver subflowResolver = (workflowUuid, triggerName, editorEnvironment) -> new Subflow(
            EncodingUtils.base64EncodeToString(workflowUuid), "inputs");

        return new WorkflowTestConfiguration().workflowSimulationFacade(
            new StandardEnvironment(), SpelEvaluator.create(), objectMapper, subflowResolver,
            new SimpleAsyncTaskExecutor(), createTaskHandlerRegistry(), workflowService);
    }

    private final class RecordingTaskHandler extends AbstractTaskHandler {

        private RecordingTaskHandler(String actionName) {
            super(TRAP, 1, actionName, actionDefinitionFacade);
        }

        @Override
        public Object handle(TaskExecution taskExecution) throws TaskExecutionException {
            handledTasks.add(
                taskExecution.getName() + ":" +
                    MapUtils.getBoolean(taskExecution.getMetadata(), MetadataConstants.DRY_RUN, false));
            handledParameters.put(taskExecution.getName(), new HashMap<>(taskExecution.getParameters()));

            return super.handle(taskExecution);
        }
    }
}
