
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

package com.bytechef.hermes.definition.registry.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.definition.registry.component.InputParametersImpl;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookDisableConsumer;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerDisableConsumer;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerEnableConsumer;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.component.factory.ContextConnectionFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionServiceImpl implements TriggerDefinitionService {

    private final List<ComponentDefinition> componentDefinitions;
    private final ContextConnectionFactory contextConnectionFactory;

    @SuppressFBWarnings("EI2")
    public TriggerDefinitionServiceImpl(
        List<ComponentDefinition> componentDefinitions, ContextConnectionFactory contextConnectionFactory) {

        this.componentDefinitions = componentDefinitions;
        this.contextConnectionFactory = contextConnectionFactory;
    }

    @Override
    public void executeDynamicWebhookDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId,
        DynamicWebhookEnableOutput output) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        TriggerDefinition triggerDefinition = getTriggerDefinition(triggerName, componentDefinition);

        DynamicWebhookDisableConsumer dynamicWebhookDisableConsumer = OptionalUtils.get(
            triggerDefinition.getDynamicWebhookDisable());

        TriggerDefinition.DynamicWebhookDisableContext context = new TriggerDefinition.DynamicWebhookDisableContext(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters), output, workflowExecutionId);

        dynamicWebhookDisableConsumer.accept(context);
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String webhookUrl,
        String workflowExecutionId) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        TriggerDefinition triggerDefinition = getTriggerDefinition(triggerName, componentDefinition);

        DynamicWebhookEnableFunction dynamicWebhookEnableFunction = OptionalUtils.get(
            triggerDefinition.getDynamicWebhookEnable());

        TriggerDefinition.DynamicWebhookEnableContext context = new TriggerDefinition.DynamicWebhookEnableContext(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters), webhookUrl, workflowExecutionId);

        return dynamicWebhookEnableFunction.apply(context);
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, DynamicWebhookEnableOutput output) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        TriggerDefinition triggerDefinition = getTriggerDefinition(triggerName, componentDefinition);

        TriggerDefinition.DynamicWebhookRefreshFunction dynamicWebhookRefreshFunction = OptionalUtils.get(
            triggerDefinition.getDynamicWebhookRefresh());

        return dynamicWebhookRefreshFunction.apply(output);
    }

    @Override
    public void executeListenerDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        TriggerDefinition triggerDefinition = getTriggerDefinition(triggerName, componentDefinition);

        ListenerDisableConsumer listenerDisableConsumer = OptionalUtils.get(triggerDefinition.getListenerDisable());

        listenerDisableConsumer.accept(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters), workflowExecutionId);
    }

    @Override
    public void executeListenerEnable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        TriggerDefinition triggerDefinition = getTriggerDefinition(triggerName, componentDefinition);

        ListenerEnableConsumer listenerEnableConsumer = OptionalUtils.get(triggerDefinition.getListenerEnable());

        listenerEnableConsumer.accept(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters), workflowExecutionId);
    }

    @Override
    public TriggerDefinitionDTO getTriggerDefinition(String componentName, int componentVersion, String triggerName) {
        try {
            return getTriggerDefinitionMono(componentName, componentVersion, triggerName)
                .toFuture()
                .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Mono<TriggerDefinitionDTO> getTriggerDefinitionMono(
        String componentName, int componentVersion, String triggerName) {

        return Mono.just(
            componentDefinitions.stream()
                .filter(componentDefinition -> triggerName.equalsIgnoreCase(
                    componentDefinition.getName()) && componentVersion == componentDefinition.getVersion())
                .flatMap(componentDefinition -> CollectionUtils.stream(
                    OptionalUtils.orElse(componentDefinition.getTriggers(), Collections.emptyList())))
                .filter(triggerDefinition -> triggerName.equalsIgnoreCase(triggerDefinition.getName()))
                .findFirst()
                .map(this::toTriggerDefinitionDTO)
                .orElseThrow(IllegalArgumentException::new));
    }

    @Override
    public Mono<List<TriggerDefinitionDTO>> getTriggerDefinitions(
        String componentName, int componentVersion) {
        return Mono.just(
            componentDefinitions.stream()
                .filter(componentDefinition -> componentName.equalsIgnoreCase(
                    componentDefinition.getName()) && componentVersion == componentDefinition.getVersion())
                .filter(componentDefinition -> OptionalUtils.isPresent(componentDefinition.getTriggers()))
                .flatMap(componentDefinition -> CollectionUtils.stream(
                    OptionalUtils.orElse(componentDefinition.getTriggers(), Collections.emptyList())))
                .map(this::toTriggerDefinitionDTO)
                .toList());
    }

    private ComponentDefinition getComponentDefinition(String componentName, int componentVersion) {
        return componentDefinitions.stream()
            .filter(curComponentDefinition -> componentName.equalsIgnoreCase(curComponentDefinition.getName()) &&
                componentVersion == curComponentDefinition.getVersion())
            .findFirst()
            .orElseThrow();
    }

    private static TriggerDefinition getTriggerDefinition(String triggerName, ComponentDefinition componentDefinition) {
        List<? extends TriggerDefinition> triggerDefinitions = OptionalUtils.get(componentDefinition.getTriggers());

        return CollectionUtils.getFirst(
            triggerDefinitions, curTriggerDefinition -> triggerName.equalsIgnoreCase(curTriggerDefinition.getName()));
    }

    private TriggerDefinitionDTO toTriggerDefinitionDTO(TriggerDefinition triggerDefinition) {
        return new TriggerDefinitionDTO(
            OptionalUtils.orElse(triggerDefinition.getBatch(), false), triggerDefinition.getDescription(),
            triggerDefinition.getExampleOutput(), OptionalUtils.orElse(triggerDefinition.getHelp(), null),
            triggerDefinition.getName(),
            OptionalUtils.orElse(triggerDefinition.getOutputSchema(), Collections.emptyList()),
            OptionalUtils.orElse(triggerDefinition.getProperties(), Collections.emptyList()),
            triggerDefinition.getTitle(), triggerDefinition.getType());
    }
}
