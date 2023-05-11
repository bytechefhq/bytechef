
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

package com.bytechef.hermes.component.registrar.handler;

import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.TriggerContext;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRequestContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRequestFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.PollContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.PollFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.StaticWebhookRequestContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.StaticWebhookRequestFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookHeaders;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookParameters;
import com.bytechef.hermes.component.util.ComponentContextSupplier;
import com.bytechef.hermes.constant.MetadataConstants;
import com.bytechef.hermes.data.storage.service.DataStorageService;
import com.bytechef.hermes.definition.registry.component.factory.ContextFactory;
import com.bytechef.hermes.definition.registry.component.factory.InputParametersFactory;
import com.bytechef.hermes.domain.TriggerExecution;
import com.bytechef.hermes.worker.trigger.excepton.TriggerExecutionException;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.component.definition.TriggerDefinition.WebhookValidateContext;

/**
 * @author Ivica Cardic
 */
public class DefaultComponentTriggerHandler implements TriggerHandler<Object> {

    private static final String BODY = "body";
    private static final String HEADERS = "headers";
    private static final String METHOD = "method";
    private static final String PARAMETERS = "parameters";
    private static final String PATH = "path";

    private final ComponentDefinitionFactory componentDefinitionFactory;
    private final ContextFactory contextFactory;
    private final DataStorageService datStorageService;
    private final InputParametersFactory inputParametersFactory;
    private final TriggerDefinition triggerDefinition;

    @SuppressFBWarnings("EI")
    public DefaultComponentTriggerHandler(
        ComponentDefinitionFactory componentDefinitionFactory, ContextFactory contextFactory,
        DataStorageService datStorageService, InputParametersFactory inputParametersFactory,
        TriggerDefinition triggerDefinition) {

        this.componentDefinitionFactory = componentDefinitionFactory;
        this.contextFactory = contextFactory;
        this.datStorageService = datStorageService;
        this.inputParametersFactory = inputParametersFactory;
        this.triggerDefinition = triggerDefinition;
    }

    @Override
    public Object handle(TriggerExecution triggerExecution) throws TriggerExecutionException {
        TriggerContext context = contextFactory.createTriggerContext(
            MapValueUtils.getMap(triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS));

        return ComponentContextSupplier.get(
            context, componentDefinitionFactory.getDefinition(), () -> doHandle(triggerExecution, context));
    }

    private Object doHandle(TriggerExecution triggerExecution, TriggerContext triggerContext)
        throws TriggerExecutionException {

        Object output;
        TriggerType triggerType = triggerDefinition.getType();
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        if ((TriggerType.WEBHOOK_DYNAMIC == triggerType || TriggerType.WEBHOOK_STATIC == triggerType) &&
            !validateWebhook(triggerContext, triggerExecution)) {

            throw new TriggerExecutionException("Invalid trigger signature.");
        }

        if (TriggerType.WEBHOOK_DYNAMIC == triggerType) {
            DynamicWebhookRequestFunction dynamicWebhookRequestFunction = OptionalUtils.get(
                triggerDefinition.getDynamicWebhookRequest());

            WebhookOutput webhookOutput = dynamicWebhookRequestFunction.apply(
                new DynamicWebhookRequestContext(
                    triggerContext, inputParametersFactory.createInputParameters(triggerExecution.getParameters()),
                    MapValueUtils.get(triggerExecution.getParameters(), HEADERS, WebhookHeaders.class),
                    MapValueUtils.get(triggerExecution.getParameters(), PARAMETERS, WebhookParameters.class),
                    MapValueUtils.get(triggerExecution.getParameters(), BODY, WebhookBody.class),
                    MapValueUtils.getRequiredString(triggerExecution.getParameters(), PATH),
                    MapValueUtils.getRequired(triggerExecution.getParameters(), METHOD, WebhookMethod.class),
                    OptionalUtils.orElse(
                        datStorageService.fetchValue(
                            Context.DataStorageScope.WORKFLOW_INSTANCE, workflowExecutionId.getInstanceId(),
                            workflowExecutionId.toString()),
                        null)));

            output = webhookOutput.getValue();
        } else if (TriggerType.WEBHOOK_STATIC == triggerType) {
            StaticWebhookRequestFunction staticWebhookRequestFunction = OptionalUtils.get(
                triggerDefinition.getStaticWebhookRequest());

            WebhookOutput webhookOutput = staticWebhookRequestFunction.apply(
                new StaticWebhookRequestContext(
                    triggerContext, inputParametersFactory.createInputParameters(triggerExecution.getParameters()),
                    MapValueUtils.get(triggerExecution.getParameters(), HEADERS, WebhookHeaders.class),
                    MapValueUtils.get(triggerExecution.getParameters(), PARAMETERS, WebhookParameters.class),
                    MapValueUtils.get(triggerExecution.getParameters(), BODY, WebhookBody.class),
                    MapValueUtils.getRequiredString(triggerExecution.getParameters(), PATH),
                    MapValueUtils.getRequired(triggerExecution.getParameters(), METHOD, WebhookMethod.class)));

            output = webhookOutput.getValue();
        } else if (TriggerType.POLLING == triggerType || TriggerType.HYBRID_DYNAMIC == triggerType) {
            PollFunction pollFunction = OptionalUtils.get(triggerDefinition.getPoll());

            PollOutput pollOutput = pollFunction.apply(
                new PollContext(
                    triggerContext, inputParametersFactory.createInputParameters(triggerExecution.getParameters()),
                    OptionalUtils.orElse(
                        datStorageService.fetchValue(
                            Context.DataStorageScope.WORKFLOW_INSTANCE, workflowExecutionId.getInstanceId(),
                            workflowExecutionId.toString()),
                        null)));

            List<Map<?, ?>> records = new ArrayList<>(
                pollOutput.records() == null ? Collections.emptyList() : pollOutput.records());

            while (pollOutput.pollImmediately()) {
                pollOutput = pollFunction.apply(
                    new PollContext(
                        triggerContext, inputParametersFactory.createInputParameters(triggerExecution.getParameters()),
                        pollOutput.closureParameters()));

                records.addAll(pollOutput.records());
            }

            if (pollOutput.closureParameters() != null) {
                datStorageService.save(
                    Context.DataStorageScope.WORKFLOW_INSTANCE, workflowExecutionId.getInstanceId(),
                    workflowExecutionId.toString(),
                    pollOutput.closureParameters());
            }

            output = records;
        } else {
            throw new TriggerExecutionException("Unknown trigger type: " + triggerType);
        }

        return output;
    }

    private Boolean validateWebhook(TriggerContext triggerContext, TriggerExecution triggerExecution) {
        WebhookValidateContext context = new WebhookValidateContext(
            triggerContext, inputParametersFactory.createInputParameters(triggerExecution.getParameters()),
            MapValueUtils.get(triggerExecution.getParameters(), HEADERS, WebhookHeaders.class),
            MapValueUtils.get(triggerExecution.getParameters(), PARAMETERS, WebhookParameters.class),
            MapValueUtils.get(triggerExecution.getParameters(), BODY, WebhookBody.class),
            MapValueUtils.getRequiredString(triggerExecution.getParameters(), PATH),
            MapValueUtils.getRequired(triggerExecution.getParameters(), METHOD, WebhookMethod.class));

        return triggerDefinition.getWebhookValidate()
            .map(webhookValidateFunction -> webhookValidateFunction.apply(context))
            .orElse(true);
    }
}
