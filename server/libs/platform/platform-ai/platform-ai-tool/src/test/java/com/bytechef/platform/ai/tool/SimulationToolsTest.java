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

package com.bytechef.platform.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationFacade;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationResult;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationResult.Outcome;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SimulationToolsTest {

    @Mock
    private WorkflowSimulationFacade workflowSimulationFacade;

    private SimulationTools simulationTools;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        simulationTools = new SimulationTools(createProvider(workflowSimulationFacade));
    }

    @Test
    void simulateWorkflowReturnsCompletedOutcome() {
        WorkflowSimulationResult workflowSimulationResult = new WorkflowSimulationResult(
            Outcome.COMPLETED, null, null, null, List.of());

        when(workflowSimulationFacade.simulate(eq("workflowId"), any()))
            .thenReturn(workflowSimulationResult);

        String result = simulationTools.simulateWorkflow("workflowId");

        WorkflowSimulationResult parsedWorkflowSimulationResult = JsonUtils.read(
            result, WorkflowSimulationResult.class);

        assertThat(parsedWorkflowSimulationResult.outcome()).isEqualTo(Outcome.COMPLETED);
        assertThat(parsedWorkflowSimulationResult.failedTaskName()).isNull();
        assertThat(parsedWorkflowSimulationResult.reason()).isNull();
    }

    @Test
    void simulateWorkflowReturnsFailedOutcome() {
        WorkflowSimulationResult workflowSimulationResult = new WorkflowSimulationResult(
            Outcome.FAILED, "task1", "action", "boom", List.of());

        when(workflowSimulationFacade.simulate(eq("workflowId"), any()))
            .thenReturn(workflowSimulationResult);

        String result = simulationTools.simulateWorkflow("workflowId");

        WorkflowSimulationResult parsedWorkflowSimulationResult = JsonUtils.read(
            result, WorkflowSimulationResult.class);

        assertThat(parsedWorkflowSimulationResult.outcome()).isEqualTo(Outcome.FAILED);
        assertThat(parsedWorkflowSimulationResult.failedTaskName()).isEqualTo("task1");
        assertThat(parsedWorkflowSimulationResult.failedTaskType()).isEqualTo("action");
        assertThat(parsedWorkflowSimulationResult.reason()).isEqualTo("boom");
    }

    @Test
    void simulateWorkflowReturnsNotAvailableWhenFacadeAbsent() {
        SimulationTools simulationToolsWithoutFacade = new SimulationTools(createProvider(null));

        String result = simulationToolsWithoutFacade.simulateWorkflow("workflowId");

        @SuppressWarnings("unchecked")
        Map<String, String> parsedResult = JsonUtils.read(result, Map.class);

        assertThat(parsedResult).containsEntry("error", "Workflow simulation is not available in this deployment.");
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<WorkflowSimulationFacade> createProvider(WorkflowSimulationFacade facade) {
        ObjectProvider<WorkflowSimulationFacade> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(facade);

        return provider;
    }
}
