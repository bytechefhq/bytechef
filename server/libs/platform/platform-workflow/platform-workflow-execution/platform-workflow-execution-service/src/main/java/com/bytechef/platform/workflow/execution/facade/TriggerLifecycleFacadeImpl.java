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

package com.bytechef.platform.workflow.execution.facade;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class TriggerLifecycleFacadeImpl implements TriggerLifecycleFacade {

    private static final Logger log = LoggerFactory.getLogger(TriggerLifecycleFacadeImpl.class);

    private static final Set<String> WEBHOOK_ENABLE_OUTPUT_PROPERTY_NAMES = Arrays.stream(
        WebhookEnableOutput.class.getRecordComponents())
        .map(RecordComponent::getName)
        .collect(Collectors.toUnmodifiableSet());

    private final TriggerScheduler triggerScheduler;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerStateService triggerStateService;

    @SuppressFBWarnings("EI")
    public TriggerLifecycleFacadeImpl(
        TriggerScheduler triggerScheduler, TriggerDefinitionFacade triggerDefinitionFacade,
        TriggerDefinitionService triggerDefinitionService, TriggerStateService triggerStateService) {

        this.triggerScheduler = triggerScheduler;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerStateService = triggerStateService;
    }

    @Override
    public boolean executeTriggerDisable(
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, @Nullable Long connectionId) {

        try {
            TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                triggerWorkflowNodeType.operation());

            switch (triggerDefinition.getType()) {
                case HYBRID, DYNAMIC_WEBHOOK -> {
                    Map<String, ?> parameters = OptionalUtils.mapOrElse(
                        triggerStateService.fetchValue(workflowExecutionId),
                        WebhookEnableOutput::parameters, Map.of());

                    triggerDefinitionFacade.executeWebhookDisable(
                        triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                        triggerWorkflowNodeType.operation(), triggerParameters, workflowExecutionId.toString(),
                        parameters, connectionId);

                    cleanUpWebhook(workflowExecutionId, triggerWorkflowNodeType);
                }
                case LISTENER -> triggerDefinitionFacade.executeListenerDisable(
                    triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                    triggerWorkflowNodeType.operation(), triggerParameters,
                    workflowExecutionId.toString(), connectionId);
                case POLLING -> triggerScheduler.cancelPollingTrigger(workflowExecutionId.toString());
                default -> {
                }
            }

            if (log.isDebugEnabled()) {
                log.debug(
                    "Trigger type='{}', name='{}', workflowExecutionId={} disabled",
                    triggerWorkflowNodeType, workflowExecutionId.getTriggerName(), workflowExecutionId);
            }

            return true;
        } catch (Exception e) {
            log.error(
                "Error while disabling trigger type='{}', name='{}', workflowExecutionId={}",
                triggerWorkflowNodeType, workflowExecutionId.getTriggerName(), workflowExecutionId, e);

            return false;
        }
    }

    @Override
    public boolean executeTriggerEnableUndo(
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, @Nullable Long connectionId, WebhookEnableOutput enableOutput,
        @Nullable Object previousTriggerState) {

        WebhookEnableOutput previousOutput =
            toWebhookTriggerState(workflowId, workflowExecutionId, previousTriggerState);

        return unsubscribeWebhook(
            workflowExecutionId, triggerWorkflowNodeType, triggerParameters, connectionId, enableOutput, previousOutput,
            exception -> log.error(
                "Error while undoing the enable of trigger type='{}', name='{}', workflowExecutionId={}",
                triggerWorkflowNodeType, workflowExecutionId.getTriggerName(), workflowExecutionId, exception));
    }

    @Override
    public @Nullable WebhookEnableOutput executeTriggerEnable(
        String workflowId, WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, Long connectionId, String webhookUrl, long environmentId) {

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(), triggerWorkflowNodeType.operation());

        WebhookEnableOutput enableOutput = null;

        switch (triggerDefinition.getType()) {
            case DYNAMIC_WEBHOOK, HYBRID, STATIC_WEBHOOK -> {
                Object previousTriggerState = triggerStateService.fetchValue(workflowExecutionId)
                    .orElse(null);

                WebhookEnableOutput previousOutput = toWebhookTriggerState(
                    workflowId, workflowExecutionId, previousTriggerState);

                WebhookEnableOutput output =
                    triggerDefinitionFacade.executeWebhookEnable(
                        triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                        triggerWorkflowNodeType.operation(), triggerParameters,
                        workflowExecutionId.toString(), connectionId, webhookUrl, environmentId);

                if (output != null) {
                    try {
                        triggerStateService.save(workflowExecutionId, output);

                        if (output.webhookExpirationDate() != null) {
                            triggerScheduler.scheduleDynamicWebhookTriggerRefresh(
                                output.webhookExpirationDate(), triggerWorkflowNodeType.name(),
                                triggerWorkflowNodeType.version(), workflowExecutionId, connectionId);
                        }
                    } catch (RuntimeException exception) {
                        unsubscribeWebhook(
                            workflowExecutionId, triggerWorkflowNodeType, triggerParameters, connectionId, output,
                            previousOutput, exception::addSuppressed);

                        throw exception;
                    }
                }

                enableOutput = output;
            }
            case LISTENER -> triggerDefinitionFacade.executeListenerEnable(
                triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                triggerWorkflowNodeType.operation(), triggerParameters, workflowExecutionId.toString(),
                connectionId);
            case POLLING -> triggerScheduler.schedulePollingTrigger(workflowExecutionId);
            default -> {
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(
                "Trigger type='{}', name='{}', workflowExecutionId={} enabled",
                triggerWorkflowNodeType, workflowExecutionId.getTriggerName(), workflowExecutionId);
        }

        return enableOutput;
    }

    private void cleanUpWebhook(WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType) {
        try {
            triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        } catch (RuntimeException exception) {
            log.error(
                "Error while cancelling the refresh of disabled trigger type='{}', name='{}', workflowExecutionId={}",
                triggerWorkflowNodeType, workflowExecutionId.getTriggerName(), workflowExecutionId, exception);
        }

        try {
            triggerStateService.delete(workflowExecutionId);
        } catch (RuntimeException exception) {
            log.error(
                "Error while deleting the state of disabled trigger type='{}', name='{}', workflowExecutionId={}",
                triggerWorkflowNodeType, workflowExecutionId.getTriggerName(), workflowExecutionId, exception);
        }
    }

    private static @Nullable WebhookEnableOutput toWebhookTriggerState(
        String workflowId, WorkflowExecutionId workflowExecutionId, @Nullable Object triggerState) {

        if (triggerState == null || triggerState instanceof WebhookEnableOutput) {
            return (WebhookEnableOutput) triggerState;
        }

        try {
            if (triggerState instanceof Map<?, ?> triggerStateMap &&
                !WEBHOOK_ENABLE_OUTPUT_PROPERTY_NAMES.containsAll(triggerStateMap.keySet())) {

                throw new IllegalArgumentException(
                    "Trigger state with keys %s is not webhook trigger state".formatted(triggerStateMap.keySet()));
            }

            return ConvertUtils.convertValue(triggerState, WebhookEnableOutput.class);
        } catch (RuntimeException exception) {
            log.warn(
                "Ignoring the stored state of trigger {} of workflow {} as it cannot be read as webhook trigger state",
                workflowExecutionId.getTriggerName(), workflowId, exception);

            return null;
        }
    }

    private boolean unsubscribeWebhook(
        WorkflowExecutionId workflowExecutionId, WorkflowNodeType triggerWorkflowNodeType,
        Map<String, ?> triggerParameters, @Nullable Long connectionId, WebhookEnableOutput output,
        @Nullable WebhookEnableOutput previousOutput, Consumer<RuntimeException> failureHandler) {

        try {
            triggerDefinitionFacade.executeWebhookDisable(
                triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(), triggerWorkflowNodeType.operation(),
                triggerParameters, workflowExecutionId.toString(), output.parameters(), connectionId);
        } catch (RuntimeException exception) {
            failureHandler.accept(exception);

            return false;
        }

        try {
            triggerScheduler.cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        } catch (RuntimeException exception) {
            failureHandler.accept(exception);
        }

        try {
            if (previousOutput == null) {
                triggerStateService.delete(workflowExecutionId);

                return true;
            }

            triggerStateService.save(workflowExecutionId, previousOutput);
        } catch (RuntimeException exception) {
            failureHandler.accept(exception);

            return true;
        }

        if (previousOutput.webhookExpirationDate() == null) {
            return true;
        }

        try {
            triggerScheduler.scheduleDynamicWebhookTriggerRefresh(
                previousOutput.webhookExpirationDate(), triggerWorkflowNodeType.name(),
                triggerWorkflowNodeType.version(), workflowExecutionId, connectionId);
        } catch (RuntimeException exception) {
            failureHandler.accept(exception);
        }

        return true;
    }
}
