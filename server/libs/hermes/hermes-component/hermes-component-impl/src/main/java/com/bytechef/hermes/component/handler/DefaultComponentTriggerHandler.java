
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

package com.bytechef.hermes.component.handler;

import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.TriggerContext;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRequestContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.PollContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.StaticWebhookRequestContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookHeaders;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookParameters;
import com.bytechef.hermes.execution.service.TriggerStateService;
import com.bytechef.hermes.definition.registry.component.util.ComponentContextSupplier;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.component.context.factory.ContextFactory;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.worker.trigger.excepton.TriggerExecutionException;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler;
import com.bytechef.hermes.execution.WorkflowExecutionId;
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

    private final ContextFactory contextFactory;
    private final TriggerStateService triggerStateService;
    private final TriggerDefinition triggerDefinition;

    @SuppressFBWarnings("EI")
    public DefaultComponentTriggerHandler(
        TriggerDefinition triggerDefinition, TriggerStateService triggerStateService,
        ContextFactory contextFactory) {

        this.contextFactory = contextFactory;
        this.triggerStateService = triggerStateService;
        this.triggerDefinition = triggerDefinition;
    }

    @Override
    public Object handle(TriggerExecution triggerExecution) throws TriggerExecutionException {
        TriggerContext context = contextFactory.createTriggerContext(
            MapValueUtils.getMap(triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class));

        return ComponentContextSupplier.get(context, () -> doHandle(triggerExecution, context));
    }

    private Object doHandle(TriggerExecution triggerExecution, TriggerContext triggerContext)
        throws TriggerExecutionException {

        Object output;
        TriggerType triggerType = triggerDefinition.getType();
        WorkflowExecutionId workflowExecutionId = triggerExecution.getWorkflowExecutionId();

        if ((TriggerType.DYNAMIC_WEBHOOK == triggerType || TriggerType.STATIC_WEBHOOK == triggerType) &&
            !validateWebhook(triggerContext, triggerExecution)) {

            throw new TriggerExecutionException("Invalid trigger signature.");
        }

        if (TriggerType.DYNAMIC_WEBHOOK == triggerType) {
            output = triggerDefinition.getDynamicWebhookRequest()
                .map(dynamicWebhookRequestFunction -> {
                    WebhookOutput webhookOutput = dynamicWebhookRequestFunction.apply(
                        new DynamicWebhookRequestContext(
                            triggerExecution.getParameters(),
                            MapValueUtils.get(triggerExecution.getParameters(), HEADERS, WebhookHeaders.class),
                            MapValueUtils.get(triggerExecution.getParameters(), PARAMETERS, WebhookParameters.class),
                            MapValueUtils.get(triggerExecution.getParameters(), BODY, WebhookBody.class),
                            MapValueUtils.getRequired(triggerExecution.getParameters(), METHOD, WebhookMethod.class),
                            OptionalUtils.orElse(triggerStateService.fetchValue(workflowExecutionId), null),
                            triggerContext));

                    return webhookOutput.getValue();
                })
                .orElseThrow();
        } else if (TriggerType.STATIC_WEBHOOK == triggerType) {
            output = triggerDefinition.getStaticWebhookRequest()
                .map(staticWebhookRequestFunction -> {
                    WebhookOutput webhookOutput = staticWebhookRequestFunction.apply(
                        new StaticWebhookRequestContext(
                            triggerExecution.getParameters(),
                            MapValueUtils.get(triggerExecution.getMetadata(), HEADERS, WebhookHeaders.class),
                            MapValueUtils.get(triggerExecution.getMetadata(), PARAMETERS, WebhookParameters.class),
                            MapValueUtils.get(triggerExecution.getMetadata(), BODY, WebhookBody.class),
                            MapValueUtils.getRequired(triggerExecution.getMetadata(), METHOD, WebhookMethod.class),
                            triggerContext));

                    return webhookOutput.getValue();
                })
                .orElseThrow();
        } else if (TriggerType.POLLING == triggerType || TriggerType.HYBRID == triggerType) {
            output = triggerDefinition.getPoll()
                .map(pollFunction -> {
                    PollOutput pollOutput = pollFunction.apply(
                        new PollContext(
                            triggerExecution.getParameters(),
                            OptionalUtils.orElse(triggerStateService.fetchValue(workflowExecutionId), null),
                            triggerContext));

                    List<Map<?, ?>> records = new ArrayList<>(
                        pollOutput.records() == null ? Collections.emptyList() : pollOutput.records());

                    while (pollOutput.pollImmediately()) {
                        pollOutput = pollFunction.apply(
                            new PollContext(
                                triggerExecution.getParameters(), pollOutput.closureParameters(), triggerContext));

                        records.addAll(pollOutput.records());
                    }

                    if (pollOutput.closureParameters() != null) {
                        triggerStateService.save(workflowExecutionId, pollOutput.closureParameters());
                    }

                    return records;
                })
                .orElseThrow();
        } else {
            throw new TriggerExecutionException("Unknown trigger type: " + triggerType);
        }

        return output;
    }

    private Boolean validateWebhook(TriggerContext triggerContext, TriggerExecution triggerExecution) {
        WebhookValidateContext context = new WebhookValidateContext(
            triggerExecution.getParameters(),
            MapValueUtils.get(triggerExecution.getParameters(), HEADERS, WebhookHeaders.class),
            MapValueUtils.get(triggerExecution.getParameters(), PARAMETERS, WebhookParameters.class),
            MapValueUtils.get(triggerExecution.getParameters(), BODY, WebhookBody.class),
            MapValueUtils.getRequired(triggerExecution.getParameters(), METHOD, WebhookMethod.class), triggerContext);

        return triggerDefinition.getWebhookValidate()
            .map(webhookValidateFunction -> webhookValidateFunction.apply(context))
            .orElse(true);
    }
}
