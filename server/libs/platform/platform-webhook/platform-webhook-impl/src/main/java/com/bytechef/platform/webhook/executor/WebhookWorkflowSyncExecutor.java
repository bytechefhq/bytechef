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

package com.bytechef.platform.webhook.executor;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcherPreSendProcessor;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class WebhookWorkflowSyncExecutor {

    private final Evaluator evaluator;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerExecutionService triggerExecutionService;
    private final List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors;
    private final TriggerFileStorage triggerFileStorage;
    private final TriggerStateService triggerStateService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WebhookWorkflowSyncExecutor(
        Evaluator evaluator, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerExecutionService triggerExecutionService,
        List<TriggerDispatcherPreSendProcessor> triggerDispatcherPreSendProcessors,
        TriggerFileStorage triggerFileStorage, TriggerStateService triggerStateService,
        WorkflowService workflowService) {

        this.evaluator = evaluator;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
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

        triggerExecution = triggerExecutionService.create(
            triggerExecution.evaluate(getInputMap(workflowExecutionId), evaluator));

        Optional<Object> valueOptional = triggerStateService.fetchValue(workflowExecutionId);

        triggerExecution.setState(valueOptional.orElse(null));

        triggerExecution = preProcess(triggerExecution);

        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId, workflowId);

        Map<String, Long> connectionIdMap = MapUtils.getMap(
            triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class, Map.of());

        TriggerOutput triggerOutput = triggerDefinitionFacade.executeTrigger(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(),
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid(),
            triggerExecution.getParameters(), triggerExecution.getState(),
            MapUtils.get(triggerExecution.getMetadata(), WebhookRequest.WEBHOOK_REQUEST, WebhookRequest.class),
            CollectionUtils.findFirstOrElse(connectionIdMap.values(), null), null, workflowExecutionId.getType(),
            false);

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

    public WebhookValidateResponse validate(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        PreProcessResult result = preProcess(workflowExecutionId, webhookRequest);

        TriggerExecution triggerExecution = result.triggerExecution();
        WorkflowNodeType workflowNodeType = result.workflowNodeType();

        Map<String, Long> connectionIdMap = result.connectionIdMap();

        return triggerDefinitionFacade.executeWebhookValidate(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(),
            triggerExecution.getParameters(), webhookRequest,
            CollectionUtils.findFirstOrElse(connectionIdMap.values(), null));
    }

    public WebhookValidateResponse validateOnEnable(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        PreProcessResult result = preProcess(workflowExecutionId, webhookRequest);

        Map<String, Long> connectIdMap = result.connectionIdMap();
        TriggerExecution triggerExecution = result.triggerExecution();
        WorkflowNodeType workflowNodeType = result.workflowNodeType();

        return triggerDefinitionFacade.executeWebhookValidateOnEnable(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(),
            triggerExecution.getParameters(), webhookRequest,
            CollectionUtils.findFirstOrElse(connectIdMap.values(), null));
    }

    private WorkflowNodeType getComponentOperation(WorkflowExecutionId workflowExecutionId, String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }

    private Map<String, ?> getInputMap(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return jobPrincipalAccessor.getInputMap(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private PreProcessResult preProcess(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        String workflowId = getWorkflowId(workflowExecutionId);

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .metadata(Map.of(WebhookRequest.WEBHOOK_REQUEST, webhookRequest))
            .workflowExecutionId(workflowExecutionId)
            .workflowTrigger(getWorkflowTrigger(workflowExecutionId, workflowId))
            .build();

        triggerExecution = preProcess(triggerExecution.evaluate(getInputMap(workflowExecutionId), evaluator));

        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId, workflowId);

        Map<String, Long> connectionIdMap = MapUtils.getMap(
            triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class, Map.of());

        return new PreProcessResult(triggerExecution, workflowNodeType, connectionIdMap);
    }

    private String getWorkflowId(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
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

    private record PreProcessResult(
        TriggerExecution triggerExecution, WorkflowNodeType workflowNodeType, Map<String, Long> connectionIdMap) {
    }
}
