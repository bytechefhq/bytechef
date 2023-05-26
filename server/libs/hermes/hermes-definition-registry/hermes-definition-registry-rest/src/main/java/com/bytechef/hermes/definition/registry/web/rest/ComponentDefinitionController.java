
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

package com.bytechef.hermes.definition.registry.web.rest;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.definition.registry.web.rest.model.ActionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ActionDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentOperationRequestModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TriggerDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TriggerDefinitionModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path:}/core")
public class ComponentDefinitionController implements ComponentDefinitionsApi {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ActionDefinitionService actionDefinitionService;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConversionService conversionService;
    private final ComponentDefinitionFacade componentDefinitionFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;

    @SuppressFBWarnings("EI")
    public ComponentDefinitionController(
        ActionDefinitionFacade actionDefinitionFacade, ActionDefinitionService actionDefinitionService,
        ConnectionDefinitionService connectionDefinitionService, ConversionService conversionService,
        ComponentDefinitionFacade componentDefinitionFacade, ComponentDefinitionService componentDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerDefinitionService triggerDefinitionService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.actionDefinitionService = actionDefinitionService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.componentDefinitionFacade = componentDefinitionFacade;
        this.conversionService = conversionService;
        this.componentDefinitionService = componentDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ComponentDefinitionModel> getComponentDefinition(
        String componentName, Integer componentVersion) {

        return ResponseEntity.ok(
            conversionService.convert(
                componentDefinitionService.getComponentDefinition(componentName, componentVersion),
                ComponentDefinitionModel.class));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ActionDefinitionModel> getComponentActionDefinition(
        String componentName, Integer componentVersion, String actionName) {

        return ResponseEntity.ok(
            conversionService.convert(
                actionDefinitionService.getComponentActionDefinition(actionName, componentName, componentVersion),
                ActionDefinitionModel.class));
    }

    @Override
    public ResponseEntity<List<ActionDefinitionBasicModel>> getComponentActionDefinitions(
        String componentName, Integer componentVersion) {

        return ResponseEntity.ok(
            actionDefinitionService.getComponentActionDefinitions(componentName, componentVersion)
                .stream()
                .map(actionDefinition -> conversionService.convert(
                    actionDefinition, ActionDefinitionBasicModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<String> getComponentActionEditorDescription(
        String componentName, Integer componentVersion, String actionName,
        ComponentOperationRequestModel componentOperationRequestModel) {

        return ResponseEntity.ok(
            actionDefinitionFacade.executeEditorDescription(
                actionName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                componentOperationRequestModel.getConnectionId()));
    }

    @Override
    public ResponseEntity<List<PropertyModel>> getComponentActionOutputSchema(
        String componentName, Integer componentVersion, String actionName,
        ComponentOperationRequestModel componentOperationRequestModel) {

        return ResponseEntity.ok(CollectionUtils.map(
            actionDefinitionFacade.executeOutputSchema(
                actionName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                componentOperationRequestModel.getConnectionId()),
            property -> conversionService.convert(property, PropertyModel.class)));
    }

    @Override
    public ResponseEntity<Object> getComponentActionSampleOutput(
        String componentName, Integer componentVersion, String actionName,
        ComponentOperationRequestModel componentOperationRequestModel) {

        return ResponseEntity.ok(triggerDefinitionFacade.executeSampleOutput(
            actionName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
            componentOperationRequestModel.getConnectionId()));
    }

    @Override
    public ResponseEntity<List<PropertyModel>> getComponentActionPropertyDynamicProperties(
        String componentName, Integer componentVersion, String actionName, String propertyName,
        ComponentOperationRequestModel componentOperationRequestModel) {

        return ResponseEntity.ok(CollectionUtils.map(
            actionDefinitionFacade.executeDynamicProperties(
                propertyName, actionName, componentName, componentVersion,
                componentOperationRequestModel.getParameters(),
                componentOperationRequestModel.getConnectionId()),
            option -> conversionService.convert(option, PropertyModel.class)));
    }

    @Override
    public ResponseEntity<List<OptionModel>> getComponentActionPropertyOptions(
        String componentName, Integer componentVersion, String actionName, String propertyName,
        ComponentOperationRequestModel componentOperationRequestModel) {

        return ResponseEntity.ok(CollectionUtils.map(
            actionDefinitionFacade.executeOptions(
                propertyName, actionName, componentName, componentVersion,
                componentOperationRequestModel.getParameters(), componentOperationRequestModel.getConnectionId()),
            option -> conversionService.convert(option, OptionModel.class)));
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<ConnectionDefinitionModel> getComponentConnectionDefinition(
        String componentName, Integer componentVersion) {

        return ResponseEntity.ok(
            conversionService.convert(
                connectionDefinitionService.getConnectionDefinition(componentName, componentVersion),
                ConnectionDefinitionModel.class));
    }

    @Override
    public ResponseEntity<List<ConnectionDefinitionBasicModel>> getComponentConnectionDefinitions(
        String componentName, Integer componentVersion) {

        return ResponseEntity.ok(
            connectionDefinitionService.getConnectionDefinitions(componentName, componentVersion)
                .stream()
                .map(connectionDefinition -> conversionService.convert(
                    connectionDefinition, ConnectionDefinitionBasicModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<ComponentDefinitionBasicModel>> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean connectionInstances,
        Boolean triggerDefinitions) {

        return ResponseEntity.ok(
            componentDefinitionFacade.searchComponentDefinitions(
                actionDefinitions, connectionDefinitions, connectionInstances, triggerDefinitions)
                .stream()
                .map(componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionBasicModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<List<ComponentDefinitionBasicModel>> getComponentDefinitionVersions(
        String componentName) {

        return ResponseEntity.ok(
            componentDefinitionService.getComponentDefinitions(componentName)
                .stream()
                .map(componentDefinition -> conversionService.convert(
                    componentDefinition, ComponentDefinitionBasicModel.class))
                .toList());
    }

    @Override
    @SuppressFBWarnings("NP")
    public ResponseEntity<TriggerDefinitionModel> getComponentTriggerDefinition(
        String componentName, Integer componentVersion, String triggerName) {

        return ResponseEntity.ok(
            conversionService.convert(
                triggerDefinitionService.getTriggerDefinition(triggerName, componentName, componentVersion),
                TriggerDefinitionModel.class));
    }

    @Override
    public ResponseEntity<List<TriggerDefinitionBasicModel>> getComponentTriggerDefinitions(
        String componentName, Integer componentVersion) {

        return ResponseEntity.ok(
            triggerDefinitionService.getTriggerDefinitions(componentName, componentVersion)
                .stream()
                .map(triggerDefinitionDTO -> conversionService.convert(
                    triggerDefinitionDTO, TriggerDefinitionBasicModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<String> getComponentTriggerEditorDescription(
        String componentName, Integer componentVersion, String triggerName,
        ComponentOperationRequestModel componentOperationRequestModel) {

        return ResponseEntity.ok(
            triggerDefinitionFacade.executeEditorDescription(
                triggerName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                componentOperationRequestModel.getConnectionId()));
    }

    @Override
    public ResponseEntity<List<PropertyModel>> getComponentTriggerOutputSchema(
        String componentName, Integer componentVersion, String triggerName,
        ComponentOperationRequestModel componentOperationRequestModel) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                triggerDefinitionFacade.executeOutputSchema(
                    triggerName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                    componentOperationRequestModel.getConnectionId()),
                property -> conversionService.convert(property, PropertyModel.class)));
    }

    @Override
    public ResponseEntity<Object> getComponentTriggerSampleOutput(
        String componentName, Integer componentVersion, String triggerName,
        ComponentOperationRequestModel componentOperationRequestModel) {

        return ResponseEntity.ok(
            triggerDefinitionFacade.executeSampleOutput(
                triggerName, componentName, componentVersion, componentOperationRequestModel.getParameters(),
                componentOperationRequestModel.getConnectionId()));
    }

    @Override
    public ResponseEntity<List<PropertyModel>> getComponentTriggerPropertyDynamicProperties(
        String componentName, Integer componentVersion, String triggerName, String propertyName,
        ComponentOperationRequestModel componentOperationRequestModel) {

        return ResponseEntity.ok(CollectionUtils.map(
            triggerDefinitionFacade.executeDynamicProperties(
                propertyName, triggerName, componentName, componentVersion,
                componentOperationRequestModel.getParameters(),
                componentOperationRequestModel.getConnectionId()),
            option -> conversionService.convert(option, PropertyModel.class)));
    }

    @Override
    public ResponseEntity<List<OptionModel>> getComponentTriggerPropertyOptions(
        String componentName, Integer componentVersion, String triggerName, String propertyName,
        ComponentOperationRequestModel componentOperationRequestModel) {

        return ResponseEntity.ok(
            CollectionUtils.map(
                triggerDefinitionFacade.executeOptions(
                    propertyName, triggerName, componentName, componentVersion,
                    componentOperationRequestModel.getParameters(),
                    componentOperationRequestModel.getConnectionId()),
                option -> conversionService.convert(option, OptionModel.class)));
    }
}
