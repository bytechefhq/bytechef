/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeDisplayConditionFacadeImpl implements WorkflowNodeDisplayConditionFacade {

    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeDisplayConditionFacadeImpl(
        WorkflowNodeOutputFacade workflowNodeOutputFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean evaluateWorkflowNodeDisplayCondition(
        String workflowId, String workflowNodeName, String displayCondition) {

        Workflow workflow = workflowService.getWorkflow(workflowId);
        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId);

        return WorkflowTrigger
            .fetch(workflow, workflowNodeName)
            .map(workflowTrigger -> evaluate(displayCondition, workflowTrigger.evaluateParameters(inputs)))
            .orElseGet(() -> {
                WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                Map<String, ?> outputs = workflowNodeOutputFacade.getWorkflowNodeSampleOutputs(
                    workflowId, workflowTask.getName());

                return evaluate(
                    displayCondition,
                    workflowTask.evaluateParameters(
                        MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs)));
            });
    }

    private boolean evaluate(String displayCondition, Map<String, ?> inputParameters) {
        Map<String, Object> result = Evaluator.evaluate(
            Map.of("displayCondition", "${" + displayCondition + "}"), inputParameters);

        Object displayConditionResult = result.get("displayCondition");

        return !(displayConditionResult instanceof String) && (boolean) displayConditionResult;
    }
}
