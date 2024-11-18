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

package com.bytechef.ee.automation.apiplatform.handler.executor;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessorRegistry;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ApiPlatformHandlerTriggerExecutor {

    private final InstanceAccessorRegistry instanceAccessorRegistry;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerExecutionService triggerExecutionService;
    private final List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors;
    private final TriggerFileStorage triggerFileStorage;
    private final TriggerStateService triggerStateService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ApiPlatformHandlerTriggerExecutor(
        InstanceAccessorRegistry instanceAccessorRegistry,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerExecutionService triggerExecutionService,
        List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors,
        TriggerFileStorage triggerFileStorage, TriggerStateService triggerStateService,
        WorkflowService workflowService) {

        this.instanceAccessorRegistry = instanceAccessorRegistry;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerExecutionService = triggerExecutionService;
        this.triggerDispatcherPreSendProcessors = triggerDispatcherPreSendProcessors;
        this.triggerFileStorage = triggerFileStorage;
        this.triggerStateService = triggerStateService;
        this.workflowService = workflowService;
    }

    public TriggerOutput execute(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        String workflowId = getWorkflowId(workflowExecutionId);

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .metadata(Map.of(WebhookRequest.WEBHOOK_REQUEST, webhookRequest))
            .workflowExecutionId(workflowExecutionId)
            .workflowTrigger(getWorkflowTrigger(workflowExecutionId, workflowId))
            .build();

        triggerExecution = triggerExecutionService.create(triggerExecution.evaluate(getInputMap(workflowExecutionId)));

        triggerExecution.setState(OptionalUtils.orElse(triggerStateService.fetchValue(workflowExecutionId), null));

        triggerExecution = preProcess(triggerExecution);

        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId, workflowId);

        Map<String, Long> connectIdMap = MapUtils.getMap(
            triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class, Map.of());

        TriggerOutput triggerOutput = triggerDefinitionFacade.executeTrigger(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName(), workflowExecutionId.getType(),
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowReferenceCode(),
            triggerExecution.getParameters(), triggerExecution.getState(),
            MapUtils.get(triggerExecution.getMetadata(), WebhookRequest.WEBHOOK_REQUEST, WebhookRequest.class),
            OptionalUtils.orElse(CollectionUtils.findFirst(connectIdMap.values()), null), false);

        triggerExecution.setBatch(triggerOutput.batch());
        triggerExecution.setOutput(
            triggerFileStorage.storeTriggerExecutionOutput(
                Validate.notNull(triggerExecution.getId(), "id"), triggerOutput.value()));
        triggerExecution.setState(triggerOutput.state());
        triggerExecution.setStatus(TriggerExecution.Status.COMPLETED);

        triggerExecutionService.update(triggerExecution);

        if (triggerExecution.getState() != null) {
            triggerStateService.save(workflowExecutionId, triggerExecution.getState());
        }

        return triggerOutput;
    }

    private WorkflowNodeType getComponentOperation(WorkflowExecutionId workflowExecutionId, String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }

    private Map<String, ?> getInputMap(WorkflowExecutionId workflowExecutionId) {
        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(workflowExecutionId.getType());

        return instanceAccessor.getInputMap(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowReferenceCode());
    }

    private String getWorkflowId(WorkflowExecutionId workflowExecutionId) {
        InstanceAccessor instanceAccessor = instanceAccessorRegistry.getInstanceAccessor(workflowExecutionId.getType());

        return instanceAccessor.getWorkflowId(
            workflowExecutionId.getInstanceId(), workflowExecutionId.getWorkflowReferenceCode());
    }

    private WorkflowTrigger getWorkflowTrigger(WorkflowExecutionId workflowExecutionId, String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return CollectionUtils.getFirst(
            WorkflowTrigger.of(workflow),
            workflowTrigger -> Objects.equals(workflowTrigger.getName(), workflowExecutionId.getTriggerName()));
    }

    private TriggerExecution preProcess(TriggerExecution triggerExecution) {
        TriggerDispatcherPreSendProcessor triggerDispatcherPreSendProcessor = null;

        for (TriggerDispatcherPreSendProcessor curTriggerDispatcherPreSendProcessor : triggerDispatcherPreSendProcessors) {
            if (curTriggerDispatcherPreSendProcessor.canProcess(triggerExecution)) {
                triggerDispatcherPreSendProcessor = curTriggerDispatcherPreSendProcessor;

                break;
            }
        }

        if (triggerDispatcherPreSendProcessor != null) {
            triggerExecution = triggerDispatcherPreSendProcessor.process(triggerExecution);
        }

        return triggerExecution;
    }
}
