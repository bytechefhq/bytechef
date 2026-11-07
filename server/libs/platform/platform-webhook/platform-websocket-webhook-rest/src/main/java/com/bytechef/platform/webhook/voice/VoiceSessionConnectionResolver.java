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

package com.bytechef.platform.webhook.voice;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.webhook.annotation.ConditionalOnWebhook;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnWebhook
public class VoiceSessionConnectionResolver {

    private final List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public VoiceSessionConnectionResolver(
        List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.triggerDispatcherPreSendProcessors = triggerDispatcherPreSendProcessors;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    public Map<String, Long> resolveDeployed(WorkflowExecutionId workflowExecutionId, WorkflowTrigger workflowTrigger) {
        TriggerExecution triggerExecution = TriggerExecution.builder()
            .workflowExecutionId(workflowExecutionId)
            .workflowTrigger(workflowTrigger)
            .build();

        for (TriggerDispatcherPreSendProcessor triggerDispatcherPreSendProcessor : triggerDispatcherPreSendProcessors) {
            if (triggerDispatcherPreSendProcessor.canProcess(triggerExecution)) {
                triggerExecution = triggerDispatcherPreSendProcessor.process(triggerExecution);

                break;
            }
        }

        return MapUtils.getMap(triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class, Map.of());
    }

    public Map<String, Long> resolveTest(String workflowId, String triggerName, long environmentId) {
        Map<String, Long> connectionIds = new LinkedHashMap<>();

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                workflowId, triggerName, environmentId);

        for (WorkflowTestConfigurationConnection workflowTestConfigurationConnection : workflowTestConfigurationConnections) {
            connectionIds.put(
                workflowTestConfigurationConnection.getWorkflowConnectionKey(),
                workflowTestConfigurationConnection.getConnectionId());
        }

        return connectionIds;
    }
}
