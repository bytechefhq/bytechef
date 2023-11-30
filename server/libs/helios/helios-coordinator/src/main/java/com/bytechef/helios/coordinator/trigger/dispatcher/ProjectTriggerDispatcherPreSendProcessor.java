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

package com.bytechef.helios.coordinator.trigger.dispatcher;

import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.coordinator.AbstractDispatcherPreSendProcessor;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.hermes.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectTriggerDispatcherPreSendProcessor extends AbstractDispatcherPreSendProcessor
    implements TriggerDispatcherPreSendProcessor {

    private final WorkflowConnectionFacade workflowConnectionFacade;

    @SuppressFBWarnings("EI")
    public ProjectTriggerDispatcherPreSendProcessor(
        ProjectInstanceWorkflowService projectInstanceWorkflowService,
        WorkflowConnectionFacade workflowConnectionFacade) {

        super(projectInstanceWorkflowService);
        this.workflowConnectionFacade = workflowConnectionFacade;
    }

    @Override
    public TriggerExecution process(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        triggerExecution.putMetadata(
            MetadataConstants.CONNECTION_IDS,
            getConnectionIdMap(workflowExecutionId.getInstanceId(), triggerExecution.getWorkflowId(),
                workflowConnectionFacade.getWorkflowConnections(triggerExecution.getWorkflowTrigger())));

        return triggerExecution;
    }
}
