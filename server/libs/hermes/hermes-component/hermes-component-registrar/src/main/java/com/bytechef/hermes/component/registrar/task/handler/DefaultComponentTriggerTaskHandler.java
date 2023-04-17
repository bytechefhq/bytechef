
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

package com.bytechef.hermes.component.registrar.task.handler;

import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.ContextImpl;
import com.bytechef.hermes.component.InputParametersImpl;
import com.bytechef.hermes.component.TriggerContext;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRequestContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRequestFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.PollContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.PollFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.StaticWebhookRequestFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.util.ComponentContextSupplier;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.hermes.trigger.TriggerExecution;
import com.bytechef.hermes.worker.trigger.excepton.TriggerExecutionException;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class DefaultComponentTriggerTaskHandler implements TriggerHandler {

    private final ComponentDefinitionFactory componentDefinitionFactory;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionService connectionService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final TriggerDefinition triggerDefinition;

    @SuppressFBWarnings("EI")
    public DefaultComponentTriggerTaskHandler(
        ComponentDefinitionFactory componentDefinitionFactory, ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService, EventPublisher eventPublisher, FileStorageService fileStorageService,
        TriggerDefinition triggerDefinition) {

        this.componentDefinitionFactory = componentDefinitionFactory;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionService = connectionService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.triggerDefinition = triggerDefinition;
    }

    @Override
    public TriggerOutput handle(TriggerExecution triggerExecution) throws TriggerExecutionException {
        TriggerContext context = new ContextImpl(
            connectionDefinitionService, connectionService, eventPublisher, fileStorageService,
            triggerExecution.getParameters(), null);

        return ComponentContextSupplier.get(
            context, componentDefinitionFactory.getDefinition(), () -> doHandle(triggerExecution, context));
    }

    private TriggerOutput doHandle(TriggerExecution triggerExecution, TriggerContext triggerContext)
        throws TriggerExecutionException {

        TriggerType triggerType = triggerDefinition.getType();

        try {
            if (triggerType == TriggerType.DYNAMIC_WEBHOOK) {
                DynamicWebhookRequestFunction dynamicWebhookRequestFunction = OptionalUtils.get(
                    triggerDefinition.getDynamicWebhookRequest());

                return dynamicWebhookRequestFunction.apply(
                    new DynamicWebhookRequestContext(
                        triggerContext,
                        new InputParametersImpl(triggerExecution.getParameters()), null, null, null, null, null,
                        null));
            } else if (triggerType == TriggerType.STATIC_WEBHOOK) {
                StaticWebhookRequestFunction staticWebhookRequestFunction = OptionalUtils.get(
                    triggerDefinition.getStaticWebhookRequest());

                return staticWebhookRequestFunction.apply(
                    new TriggerDefinition.StaticWebhookRequestContext(
                        triggerContext,
                        new InputParametersImpl(triggerExecution.getParameters()), null, null, null, null, null));
            } else if (triggerType == TriggerType.POLLING || triggerType == TriggerType.HYBRID) {
                PollFunction pollFunction = OptionalUtils.get(triggerDefinition.getPoll());

                return pollFunction.apply(
                    new PollContext(triggerContext, new InputParametersImpl(triggerExecution.getParameters()), null));
            } else {
                throw new TriggerExecutionException("Unknown trigger type: " + triggerType);
            }
        } catch (Exception e) {
            throw new TriggerExecutionException(e.getMessage(), e);
        }
    }
}
