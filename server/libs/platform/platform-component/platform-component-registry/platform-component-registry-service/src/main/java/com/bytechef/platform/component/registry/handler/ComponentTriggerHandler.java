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

package com.bytechef.platform.component.registry.handler;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.worker.trigger.exception.TriggerExecutionException;
import com.bytechef.platform.workflow.worker.trigger.handler.TriggerHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ComponentTriggerHandler implements TriggerHandler {

    private final String componentName;
    private final int componentVersion;
    private final String triggerName;
    private final TriggerDefinitionFacade triggerDefinitionFacade;

    @SuppressFBWarnings("EI")
    public ComponentTriggerHandler(
        String componentName, int componentVersion, String triggerName,
        TriggerDefinitionFacade triggerDefinitionFacade) {

        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.triggerName = triggerName;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
    }

    @Override
    public TriggerOutput handle(TriggerExecution triggerExecution) throws TriggerExecutionException {
        Map<String, Long> connectIdMap = MapUtils.getMap(
            triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class, Map.of());

        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        try {
            return triggerDefinitionFacade.executeTrigger(
                componentName, componentVersion, triggerName,
                workflowExecutionId.getType(), workflowExecutionId.getInstanceId(),
                workflowExecutionId.getWorkflowId(), null,
                triggerExecution.getParameters(), triggerExecution.getState(),
                MapUtils.get(triggerExecution.getMetadata(), WebhookRequest.WEBHOOK_REQUEST, WebhookRequest.class),
                OptionalUtils.orElse(CollectionUtils.findFirst(connectIdMap.values()), null));
        } catch (Exception e) {
            throw new TriggerExecutionException(e.getMessage(), e);
        }
    }
}
