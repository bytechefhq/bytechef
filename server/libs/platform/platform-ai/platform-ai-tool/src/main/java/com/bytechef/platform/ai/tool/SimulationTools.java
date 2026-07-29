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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationFacade;
import com.bytechef.platform.job.sync.simulation.WorkflowSimulationResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Platform-level workflow simulation tool.
 *
 * @author Ivica Cardic
 */
@Component
public class SimulationTools {

    private final ObjectProvider<WorkflowSimulationFacade> workflowSimulationFacadeProvider;

    @SuppressFBWarnings("EI")
    public SimulationTools(ObjectProvider<WorkflowSimulationFacade> workflowSimulationFacadeProvider) {
        this.workflowSimulationFacadeProvider = workflowSimulationFacadeProvider;
    }

    @Tool(
        description = "Dry-run a workflow: executes the DAG with mocked component outputs (no real calls) and returns COMPLETED or the first failing task + reason. Use to validate a workflow you just built.")
    public String simulateWorkflow(@ToolParam(description = "The id of the workflow to simulate") String workflowId) {
        WorkflowSimulationFacade workflowSimulationFacade = workflowSimulationFacadeProvider.getIfAvailable();

        if (workflowSimulationFacade == null) {
            return JsonUtils.write(Map.of("error", "Workflow simulation is not available in this deployment."));
        }

        WorkflowSimulationResult workflowSimulationResult = workflowSimulationFacade.simulate(
            workflowId, Map.of());

        return JsonUtils.write(workflowSimulationResult);
    }
}
