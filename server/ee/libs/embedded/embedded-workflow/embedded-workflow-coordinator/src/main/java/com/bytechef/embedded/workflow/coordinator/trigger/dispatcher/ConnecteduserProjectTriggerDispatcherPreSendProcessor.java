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

package com.bytechef.embedded.workflow.coordinator.trigger.dispatcher;

import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.embedded.workflow.coordinator.AbstractConnectedUserProjectDispatcherPreSendProcessor;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(2)
@ConditionalOnEEVersion
public class ConnecteduserProjectTriggerDispatcherPreSendProcessor
    extends AbstractConnectedUserProjectDispatcherPreSendProcessor implements TriggerDispatcherPreSendProcessor {

    private final ProjectWorkflowService projectWorkflowService;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @SuppressFBWarnings("EI")
    public ConnecteduserProjectTriggerDispatcherPreSendProcessor(
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry) {

        super(projectDeploymentWorkflowService);

        this.projectWorkflowService = projectWorkflowService;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
    }

    @Override
    public TriggerExecution process(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        String workflowId = projectWorkflowService.getProjectDeploymentWorkflowId(
            triggerExecution.getInstanceId(), workflowExecutionId.getWorkflowUuid());

        Map<String, Long> connectionIdMap = getConnectionIdMap(
            workflowExecutionId.getJobPrincipalId(), workflowId, triggerExecution.getName());

        if (!connectionIdMap.isEmpty()) {
            triggerExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
        }

        int environmentId = (int) jobPrincipalAccessorRegistry
            .getJobPrincipalAccessor(PlatformType.AUTOMATION)
            .getEnvironmentId(workflowExecutionId.getJobPrincipalId());
        triggerExecution.putMetadata(MetadataConstants.ENVIRONMENT_ID, environmentId);

        return triggerExecution;
    }

    @Override
    public boolean canProcess(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        return workflowExecutionId.getType() == PlatformType.AUTOMATION;
    }
}
