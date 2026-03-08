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

package com.bytechef.automation.configuration.subflow;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class SubflowResolverImpl implements SubflowResolver {

    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    SubflowResolverImpl(ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public Subflow resolveSubflow(String workflowUuid, String triggerName, boolean editorEnvironment) {
        String workflowId = resolveWorkflowId(workflowUuid, editorEnvironment);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        String inputsName = getCallableTriggerName(workflow, triggerName);

        return new Subflow(workflowId, inputsName);
    }

    private String getCallableTriggerName(Workflow workflow, String triggerName) {
        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), triggerName)) {

                return workflowTrigger.getName();
            }
        }

        throw new IllegalStateException(
            "No callable trigger found in workflow '%s'".formatted(workflow.getId()));
    }

    private String resolveWorkflowId(String workflowUuid, boolean editorEnvironment) {
        if (editorEnvironment) {
            return projectWorkflowService.getLastWorkflowId(workflowUuid);
        }

        return projectWorkflowService.getLastPublishedWorkflowId(workflowUuid);
    }
}
