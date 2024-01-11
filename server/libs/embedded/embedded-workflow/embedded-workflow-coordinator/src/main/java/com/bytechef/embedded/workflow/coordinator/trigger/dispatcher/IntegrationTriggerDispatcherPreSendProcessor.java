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

package com.bytechef.embedded.workflow.coordinator.trigger.dispatcher;

import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.embedded.workflow.coordinator.AbstractDispatcherPreSendProcessor;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
// TODD @Component
public class IntegrationTriggerDispatcherPreSendProcessor extends AbstractDispatcherPreSendProcessor
    implements TriggerDispatcherPreSendProcessor {

    @SuppressFBWarnings("EI")
    public IntegrationTriggerDispatcherPreSendProcessor(
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService) {

        super(integrationInstanceConfigurationWorkflowService);

    }

    @Override
    public TriggerExecution process(TriggerExecution triggerExecution) {
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        triggerExecution.putMetadata(
            MetadataConstants.CONNECTION_IDS,
            getConnectionIdMap(
                workflowExecutionId.getInstanceId(), triggerExecution.getWorkflowId(), triggerExecution.getName()));

        return triggerExecution;
    }
}
