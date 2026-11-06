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

package com.bytechef.platform.scheduler.db.task;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class DynamicWebhookRefresher {

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerStateService triggerStateService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public DynamicWebhookRefresher(
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerStateService triggerStateService, WorkflowService workflowService) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerStateService = triggerStateService;
        this.workflowService = workflowService;
    }

    @Nullable
    public Instant refresh(WorkflowExecutionId workflowExecutionId, long connectionId) {
        WorkflowNodeType workflowNodeType = getWorkflowNodeType(workflowExecutionId);
        WebhookEnableOutput output = OptionalUtils.get(triggerStateService.fetchValue(workflowExecutionId));

        output = triggerDefinitionFacade.executeDynamicWebhookRefresh(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), output.parameters(),
            connectionId);

        if (output == null) {
            return null;
        }

        triggerStateService.save(workflowExecutionId, output);

        return output.webhookExpirationDate();
    }

    private WorkflowNodeType getWorkflowNodeType(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        String workflowId = jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }
}
