
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
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessor;
import com.bytechef.hermes.coordinator.instance.InstanceWorkflowAccessorRegistry;
import com.bytechef.hermes.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
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

    private final InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerExecutionService triggerExecutionService;
    private final List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors;
    private final TriggerStateService triggerStateService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TriggerSyncExecutor(
        InstanceWorkflowAccessorRegistry instanceWorkflowAccessorRegistry,
        TriggerDefinitionService triggerDefinitionService, TriggerExecutionService triggerExecutionService,
        List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors,
        TriggerStateService triggerStateService, WorkflowService workflowService) {

        this.instanceWorkflowAccessorRegistry = instanceWorkflowAccessorRegistry;
        this.triggerDefinitionService = triggerDefinitionService;
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

        triggerExecution = triggerExecutionService.create(triggerExecution.evaluate(getInputs(workflowExecutionId)));

        triggerExecution.setState(OptionalUtils.orElse(triggerStateService.fetchValue(workflowExecutionId), null));

        triggerExecution = preProcess(triggerExecution);

        ComponentOperation componentOperation = getComponentOperation(workflowExecutionId);

        TriggerOutput triggerOutput = triggerDefinitionService.executeTrigger(
            componentOperation.componentName(), componentOperation.componentVersion(),
            componentOperation.operationName(), triggerExecution.getParameters(),
            triggerExecution.getState(),
            MapUtils.getRequired(triggerExecution.getMetadata(), WebhookRequest.WEBHOOK_REQUEST, WebhookRequest.class),
            MapUtils.getMap(triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class));

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

        triggerExecution = preProcess(triggerExecution.evaluate(getInputs(workflowExecutionId)));

        ComponentOperation componentOperation = getComponentOperation(workflowExecutionId);

        return triggerDefinitionService.executeWebhookValidate(
            componentOperation.componentName(), componentOperation.componentVersion(),
            componentOperation.operationName(), triggerExecution.getParameters(),
            MapUtils.getRequired(triggerExecution.getMetadata(), WebhookRequest.WEBHOOK_REQUEST, WebhookRequest.class),
            MapUtils.getMap(triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class));
    }

    private ComponentOperation getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(workflowExecutionId.getWorkflowId());

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return ComponentOperation.ofType(workflowTrigger.getType());
    }

    private Map<String, ?> getInputs(WorkflowExecutionId workflowExecutionId) {
        InstanceWorkflowAccessor instanceWorkflowAccessor = instanceWorkflowAccessorRegistry
            .getInstanceWorkflowAccessor(workflowExecutionId.getInstanceType());

        return instanceWorkflowAccessor.getInputs(
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
