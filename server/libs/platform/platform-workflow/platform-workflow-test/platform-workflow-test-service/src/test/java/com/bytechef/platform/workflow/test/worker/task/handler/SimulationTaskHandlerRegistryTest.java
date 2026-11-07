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

package com.bytechef.platform.workflow.test.worker.task.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.workflow.worker.task.handler.AbstractTaskHandler;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SimulationTaskHandlerRegistryTest {

    private static final String TYPE = "slack/v1/sendMessage";

    private final ActionDefinitionFacade actionDefinitionFacade = mock(ActionDefinitionFacade.class);

    @Test
    void testGetTaskHandlerDryRunsAnAbstractTaskHandler() throws TaskExecutionException {
        when(actionDefinitionFacade.executeDryRunPerform("slack", 1, "sendMessage")).thenReturn(Map.of("ok", true));

        SimulationTaskHandlerRegistry simulationTaskHandlerRegistry = new SimulationTaskHandlerRegistry(
            type -> new AbstractTaskHandler("slack", 1, "sendMessage", actionDefinitionFacade) {});

        TaskHandler<?> taskHandler = simulationTaskHandlerRegistry.getTaskHandler(TYPE);

        assertThat(taskHandler.handle(createTaskExecution(Map.of(MetadataConstants.DRY_RUN, true))))
            .isEqualTo(Map.of("ok", true));

        verify(actionDefinitionFacade).executeDryRunPerform("slack", 1, "sendMessage");
        verifyNoMoreInteractions(actionDefinitionFacade);
    }

    @Test
    void testGetTaskHandlerRejectsATaskWithoutTheDryRunFlag() {
        SimulationTaskHandlerRegistry simulationTaskHandlerRegistry = new SimulationTaskHandlerRegistry(
            type -> new AbstractTaskHandler("slack", 1, "sendMessage", actionDefinitionFacade) {});

        TaskHandler<?> taskHandler = simulationTaskHandlerRegistry.getTaskHandler(TYPE);

        assertThatThrownBy(() -> taskHandler.handle(createTaskExecution(Map.of())))
            .isInstanceOf(TaskExecutionException.class)
            .hasMessageContaining("without the dry-run flag");
        assertThatThrownBy(() -> taskHandler.handle(createTaskExecution(Map.of(MetadataConstants.DRY_RUN, false))))
            .isInstanceOf(TaskExecutionException.class);

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testGetTaskHandlerRejectsAHandlerOutsideTheComponentPerformPath() {
        AtomicInteger handleCalls = new AtomicInteger();

        SimulationTaskHandlerRegistry simulationTaskHandlerRegistry = new SimulationTaskHandlerRegistry(
            type -> taskExecution -> handleCalls.incrementAndGet());

        TaskHandler<?> taskHandler = simulationTaskHandlerRegistry.getTaskHandler(TYPE);

        assertThatThrownBy(() -> taskHandler.handle(createTaskExecution(Map.of(MetadataConstants.DRY_RUN, true))))
            .isInstanceOf(TaskExecutionException.class)
            .hasMessageContaining("cannot be simulated");
        assertThat(handleCalls).hasValue(0);
    }

    private static TaskExecution createTaskExecution(Map<String, ?> metadata) {
        return TaskExecution.builder()
            .jobId(1L)
            .metadata(metadata)
            .workflowTask(new WorkflowTask(Map.of("name", "sendMessage", "type", TYPE)))
            .build();
    }
}
