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

package com.bytechef.automation.ai.a2a.config;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public final class A2aIntTestWorkflows {

    private static final String NEW_WORKFLOW_CALL_DEFINITION = """
        {
            "label": "A2A skill",
            "triggers": [{"name": "newWorkflowCall_1", "type": "workflow/v1/newWorkflowCall"}],
            "tasks": []
        }
        """;

    private A2aIntTestWorkflows() {
    }

    public static String createNewWorkflowCallWorkflow(WorkflowService workflowService) {
        Workflow workflow = workflowService.create(
            NEW_WORKFLOW_CALL_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return workflow.getId();
    }

    public static void deleteWorkflows(WorkflowService workflowService, List<String> workflowIds) {
        for (String workflowId : workflowIds) {
            if (workflowService.fetchWorkflow(workflowId)
                .isPresent()) {

                workflowService.delete(workflowId);
            }
        }
    }
}
