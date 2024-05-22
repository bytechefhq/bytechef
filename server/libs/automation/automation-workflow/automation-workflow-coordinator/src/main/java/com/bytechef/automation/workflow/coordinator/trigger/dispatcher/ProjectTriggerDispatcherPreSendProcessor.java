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

package com.bytechef.automation.workflow.coordinator.trigger.dispatcher;

import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.workflow.coordinator.AbstractDispatcherPreSendProcessor;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectTriggerDispatcherPreSendProcessor extends AbstractDispatcherPreSendProcessor
    implements TriggerDispatcherPreSendProcessor {

    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectTriggerDispatcherPreSendProcessor(
        ProjectInstanceWorkflowService projectInstanceWorkflowService, ProjectWorkflowService projectWorkflowService) {

        super(projectInstanceWorkflowService);

        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public TriggerExecution process(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        String workflowId = projectWorkflowService.getProjectWorkflowId(
            triggerExecution.getInstanceId(), workflowExecutionId.getWorkflowReferenceCode());

        Map<String, Long> connectionIdMap = getConnectionIdMap(
            workflowExecutionId.getInstanceId(), workflowId, triggerExecution.getName());

        if (!connectionIdMap.isEmpty()) {
            triggerExecution.putMetadata(MetadataConstants.CONNECTION_IDS, connectionIdMap);
        }

        return triggerExecution;
    }
}
