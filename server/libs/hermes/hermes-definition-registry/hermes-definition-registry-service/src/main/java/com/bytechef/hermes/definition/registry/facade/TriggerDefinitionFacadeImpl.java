
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

package com.bytechef.hermes.definition.registry.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ContextConnectionImpl;
import com.bytechef.hermes.component.InputParametersImpl;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookDisableConsumer;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookDisableContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRefreshFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerDisableConsumer;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerEnableConsumer;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionFacadeImpl implements TriggerDefinitionFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionDefinitionService connectionDefinitionService;

    @SuppressFBWarnings("EI2")
    public TriggerDefinitionFacadeImpl(
        ComponentDefinitionService componentDefinitionService,
        ConnectionDefinitionService connectionDefinitionService) {

        this.componentDefinitionService = componentDefinitionService;
        this.connectionDefinitionService = connectionDefinitionService;
    }

    @Override
    public void executeDynamicWebhookDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId,
        DynamicWebhookEnableOutput output) {

        Definitions definitions = getDefinitions(componentName, componentVersion, triggerName);

        DynamicWebhookDisableConsumer dynamicWebhookDisableConsumer = definitions.getDynamicWebhookDisable();

        DynamicWebhookDisableContext context = new DynamicWebhookDisableContext(
            new ContextConnectionImpl(
                authorizationName, componentName, connectionDefinitionService,
                definitions.getConnectionVersion(), connectionParameters),
            new InputParametersImpl(triggerParameters), output, workflowExecutionId);

        dynamicWebhookDisableConsumer.accept(context);
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String webhookUrl,
        String workflowExecutionId) {

        Definitions definitions = getDefinitions(componentName, componentVersion, triggerName);

        DynamicWebhookEnableFunction dynamicWebhookEnableFunction = definitions.getDynamicWebhookEnable();

        DynamicWebhookEnableContext context = new DynamicWebhookEnableContext(
            new ContextConnectionImpl(
                authorizationName, componentName, connectionDefinitionService, definitions.getConnectionVersion(),
                connectionParameters),
            new InputParametersImpl(triggerParameters), webhookUrl, workflowExecutionId);

        return dynamicWebhookEnableFunction.apply(context);
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String triggerName, String componentName, int componentVersion, DynamicWebhookEnableOutput output) {

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, componentVersion);

        TriggerDefinition triggerDefinition = getTriggerDefinition(triggerName, componentDefinition);

        DynamicWebhookRefreshFunction dynamicWebhookRefreshFunction = OptionalUtils.get(
            triggerDefinition.getDynamicWebhookRefresh());

        return dynamicWebhookRefreshFunction.apply(output);
    }

    @Override
    public void executeListenerDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId) {

        Definitions definitions = getDefinitions(componentName, componentVersion, triggerName);

        ListenerDisableConsumer listenerDisableConsumer = definitions.getListenerDisable();

        listenerDisableConsumer.accept(
            new ContextConnectionImpl(
                authorizationName, componentName, connectionDefinitionService, definitions.getConnectionVersion(),
                connectionParameters),
            new InputParametersImpl(triggerParameters), workflowExecutionId);
    }

    @Override
    public void executeListenerEnable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId) {

        Definitions definitions = getDefinitions(componentName, componentVersion, triggerName);

        ListenerEnableConsumer listenerEnableConsumer = definitions.getListenerEnable();

        listenerEnableConsumer.accept(
            new ContextConnectionImpl(
                authorizationName, componentName, connectionDefinitionService, definitions.getConnectionVersion(),
                connectionParameters),
            new InputParametersImpl(triggerParameters), workflowExecutionId);
    }

    private Definitions getDefinitions(String componentName, int componentVersion, String triggerName) {
        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, componentVersion);

        TriggerDefinition triggerDefinition = getTriggerDefinition(triggerName, componentDefinition);

        ConnectionDefinition connectionDefinition = OptionalUtils.get(componentDefinition.getConnection());

        return new Definitions(triggerDefinition, connectionDefinition);
    }

    private static TriggerDefinition getTriggerDefinition(String triggerName, ComponentDefinition componentDefinition) {
        List<? extends TriggerDefinition> triggerDefinitions = OptionalUtils.get(componentDefinition.getTriggers());

        return CollectionUtils.getFirst(
            triggerDefinitions, curTriggerDefinition -> triggerName.equalsIgnoreCase(curTriggerDefinition.getName()));
    }

    private record Definitions(TriggerDefinition triggerDefinition, ConnectionDefinition connectionDefinition) {

        public int getConnectionVersion() {
            return connectionDefinition.getVersion();
        }

        public DynamicWebhookDisableConsumer getDynamicWebhookDisable() {
            return OptionalUtils.get(triggerDefinition.getDynamicWebhookDisable());
        }

        public DynamicWebhookEnableFunction getDynamicWebhookEnable() {
            return OptionalUtils.get(triggerDefinition.getDynamicWebhookEnable());
        }

        public ListenerDisableConsumer getListenerDisable() {
            return OptionalUtils.get(triggerDefinition.getListenerDisable());
        }

        public ListenerEnableConsumer getListenerEnable() {
            return OptionalUtils.get(triggerDefinition.getListenerEnable());
        }
    }
}
