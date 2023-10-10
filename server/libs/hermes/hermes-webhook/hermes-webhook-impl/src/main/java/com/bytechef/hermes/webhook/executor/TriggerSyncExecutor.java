
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.webhook.executor;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.hermes.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.service.TriggerExecutionService;
import com.bytechef.hermes.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerSyncExecutor {

    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerExecutionService triggerExecutionService;
    private final List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors;
    private final TriggerStateService triggerStateService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TriggerSyncExecutor(
        InstanceAccessorRegistry instanceAccessorRegistry,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerExecutionService triggerExecutionService,
        List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors,
        TriggerStateService triggerStateService, WorkflowService workflowService) {

        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerDispatcherPreSendProcessors = triggerDispatcherPreSendProcessors;
        this.triggerStateService = triggerStateService;
        this.workflowService = workflowService;
    }

    public TriggerOutput execute(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        TriggerExecution triggerExecution = TriggerExecution.builder()
            .metadata(Map.of(WebhookRequest.WEBHOOK_REQUEST, webhookRequest))
            .workflowExecutionId(workflowExecutionId)
            .workflowTrigger(getWorkflowTrigger(workflowExecutionId))
            .build();

        triggerExecution = triggerExecutionService.create(triggerExecution.evaluate(getInputMap(workflowExecutionId)));

        triggerExecution.setState(OptionalUtils.orElse(triggerStateService.fetchValue(workflowExecutionId), null));

        triggerExecution = preProcess(triggerExecution);

        ComponentOperation componentOperation = getComponentOperation(workflowExecutionId);

        Map<String, Long> connectIdMap = MapUtils.getMap(
            triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class, Map.of());

        TriggerOutput triggerOutput = triggerDefinitionFacade.executeTrigger(
            componentOperation.componentName(), componentOperation.componentVersion(),
            componentOperation.operationName(), triggerExecution.getParameters(),
            triggerExecution.getState(),
            MapUtils.getRequired(triggerExecution.getMetadata(), WebhookRequest.WEBHOOK_REQUEST, WebhookRequest.class),
            OptionalUtils.orElse(CollectionUtils.findFirst(connectIdMap.values()), null));

        triggerExecution.setBatch(triggerOutput.batch());
        triggerExecution.setOutput(triggerOutput.value());
        triggerExecution.setState(triggerOutput.state());
        triggerExecution.setStatus(TriggerExecution.Status.COMPLETED);

        triggerExecutionService.update(triggerExecution);

        if (triggerExecution.getState() != null) {
            triggerStateService.save(workflowExecutionId, triggerExecution.getState());
        }

        return triggerOutput;
    }

    public boolean validate(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        TriggerExecution triggerExecution = TriggerExecution.builder()
            .metadata(Map.of(WebhookRequest.WEBHOOK_REQUEST, webhookRequest))
            .workflowExecutionId(workflowExecutionId)
            .workflowTrigger(getWorkflowTrigger(workflowExecutionId))
            .build();

        triggerExecution = preProcess(triggerExecution.evaluate(getInputMap(workflowExecutionId)));

        ComponentOperation componentOperation = getComponentOperation(workflowExecutionId);

        Map<String, Long> connectIdMap = MapUtils.getMap(
            triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class, Map.of());

        return triggerDefinitionFacade.executeWebhookValidate(
            componentOperation.componentName(), componentOperation.componentVersion(),
            componentOperation.operationName(), triggerExecution.getParameters(),
            MapUtils.getRequired(triggerExecution.getMetadata(), WebhookRequest.WEBHOOK_REQUEST, WebhookRequest.class),
            OptionalUtils.orElse(CollectionUtils.findFirst(connectIdMap.values()), null));
    }

    private ComponentOperation getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(workflowExecutionId.getWorkflowId());

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return ComponentOperation.ofType(workflowTrigger.getType());
    }

    private Map<String, ?> getInputMap(WorkflowExecutionId workflowExecutionId) {
        InstanceAccessor instanceAccessor = instanceAccessorRegistry
            .getInstanceAccessor(workflowExecutionId.getInstanceType());

        return instanceAccessor.getInputMap(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowId());
    }

    private WorkflowTrigger getWorkflowTrigger(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(workflowExecutionId.getWorkflowId());

        return CollectionUtils.getFirst(
            WorkflowTrigger.of(workflow),
            workflowTrigger -> Objects.equals(workflowTrigger.getName(), workflowExecutionId.getTriggerName()));
    }

    private TriggerExecution preProcess(TriggerExecution triggerExecution) {
        for (TriggerDispatcherPreSendProcessor triggerDispatcherPreSendProcessor : triggerDispatcherPreSendProcessors) {
            triggerExecution = triggerDispatcherPreSendProcessor.process(triggerExecution);
        }

        return triggerExecution;
    }
}
