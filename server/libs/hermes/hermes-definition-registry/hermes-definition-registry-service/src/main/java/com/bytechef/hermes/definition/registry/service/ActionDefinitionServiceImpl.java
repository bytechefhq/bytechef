
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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.context.factory.ContextConnectionFactory;
import com.bytechef.hermes.component.context.factory.ContextFactory;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentOptionsFunction;
import com.bytechef.hermes.component.definition.ComponentPropertiesFunction;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource.EditorDescriptionFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.SampleOutputDataSource;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.SampleOutputFunction;
import com.bytechef.hermes.definition.DynamicOptionsProperty;
import com.bytechef.hermes.definition.OptionsDataSource;
import com.bytechef.hermes.definition.PropertiesDataSource;
import com.bytechef.hermes.definition.Property.DynamicPropertiesProperty;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.component.util.ComponentContextSupplier;
import com.bytechef.hermes.definition.registry.domain.ActionDefinition;
import com.bytechef.hermes.definition.registry.component.util.CustomActionUtils;
import com.bytechef.hermes.definition.registry.component.ComponentOperation;
import com.bytechef.hermes.definition.registry.domain.Property;
import com.bytechef.hermes.definition.registry.domain.Option;
import com.bytechef.hermes.definition.registry.domain.ValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionServiceImpl implements ActionDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ContextConnectionFactory contextConnectionFactory;
    private final ContextFactory contextFactory;

    @SuppressFBWarnings("EI2")
    public ActionDefinitionServiceImpl(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextConnectionFactory contextConnectionFactory,
        ContextFactory contextFactory) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextConnectionFactory = contextConnectionFactory;
        this.contextFactory = contextFactory;
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, Object> actionParameters, Long connectionId, Map<String, ?> connectionParameters,
        String authorizationName) {

        ComponentPropertiesFunction propertiesFunction = getComponentPropertiesFunction(
            componentName, componentVersion, actionName, propertyName);

        return ComponentContextSupplier.get(
            getActionContext(componentName, connectionId),
            () -> {
                List<? extends com.bytechef.hermes.definition.Property.ValueProperty<?>> valueProperties =
                    propertiesFunction.apply(
                        contextConnectionFactory.createConnection(
                            componentName, componentVersion, connectionParameters, authorizationName),
                        actionParameters);

                return valueProperties.stream()
                    .map(valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty))
                    .toList();
            });
    }

    @Override
    public String executeEditorDescription(
        String componentName, int componentVersion, String actionName, Map<String, ?> actionParameters,
        Long connectionId, Map<String, ?> connectionParameters, String authorizationName) {

        EditorDescriptionFunction editorDescriptionFunction = getEditorDescriptionFunction(
            componentName, componentVersion, actionName);

        return ComponentContextSupplier.get(
            getActionContext(componentName, connectionId),
            () -> editorDescriptionFunction.apply(
                contextConnectionFactory.createConnection(
                    componentName, componentVersion, connectionParameters, authorizationName),
                actionParameters));
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, Object> actionParameters, String searchText, Long connectionId, Map<String, ?> connectionParameters,
        String authorizationName) {

        ComponentOptionsFunction optionsFunction = getComponentOptionsFunction(
            componentName, componentVersion, actionName, propertyName);

        return ComponentContextSupplier.get(
            getActionContext(componentName, connectionId),
            () -> {
                List<com.bytechef.hermes.definition.Option<?>> options = optionsFunction.apply(
                    contextConnectionFactory.createConnection(
                        componentName, componentVersion, connectionParameters, authorizationName),
                    actionParameters, searchText);

                return options.stream()
                    .map(Option::new)
                    .toList();
            });
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        String componentName, int componentVersion, String actionName, Map<String, Object> actionParameters,
        Long connectionId, Map<String, ?> connectionParameters, String authorizationName) {

        OutputSchemaFunction outputSchemaFunction = getOutputSchemaFunction(
            componentName, componentVersion, actionName);

        return ComponentContextSupplier.get(
            getActionContext(componentName, connectionId),
            () -> {
                return Property.toProperty(
                    outputSchemaFunction.apply(
                        contextConnectionFactory.createConnection(
                            componentName, componentVersion, connectionParameters, authorizationName),
                        actionParameters));
            });
    }

    @Override
    public Object executePerform(
        String componentName, int componentVersion, String actionName, long taskExecutionId,
        Map<String, ?> inputParameters, Map<String, Long> connectionIdMap) {

        com.bytechef.hermes.component.definition.ActionDefinition actionDefinition =
            resolveActionDefinition(componentName, componentVersion, actionName);
        ActionContext context = contextFactory.createActionContext(connectionIdMap, taskExecutionId);

        return ComponentContextSupplier.get(
            context,
            () -> OptionalUtils.map(
                actionDefinition.getPerform(), performFunction -> performFunction.apply(inputParameters, context)));
    }

    @Override
    public Object executeSampleOutput(
        String componentName, int componentVersion, String actionName, Map<String, Object> actionParameters,
        Long connectionId, Map<String, ?> connectionParameters, String authorizationName) {

        SampleOutputFunction sampleOutputFunction = getSampleOutputFunction(
            componentName, componentVersion, actionName);

        return ComponentContextSupplier.get(
            getActionContext(componentName, connectionId),
            () -> sampleOutputFunction.apply(
                contextConnectionFactory.createConnection(
                    componentName, componentVersion, connectionParameters, authorizationName),
                actionParameters));
    }

    @Override
    public ActionDefinition getActionDefinition(String componentName, int componentVersion, String actionName) {
        return new ActionDefinition(resolveActionDefinition(componentName, componentVersion, actionName));
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        List<ActionDefinition> actions =
            componentDefinitionRegistry.getActionDefinitions(componentName, componentVersion)
                .stream()
                .map(ActionDefinition::new)
                .toList();

        if (OptionalUtils.orElse(componentDefinition.getCustomAction(), false)) {
            actions = new ArrayList<>(actions);

            actions.add(new ActionDefinition(CustomActionUtils.getCustomActionDefinition(componentDefinition)));
        }

        return actions;
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(List<ComponentOperation> componentOperations) {
        return componentOperations.stream()
            .map(componentOperation -> getActionDefinition(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName()))
            .toList();
    }

    private ActionContext getActionContext(String componentName, Long connectionId) {
        return contextFactory.createActionContext(
            connectionId == null ? Map.of() : Map.of(componentName, connectionId));
    }

    private ComponentOptionsFunction getComponentOptionsFunction(
        String componentName, int componentVersion, String actionName, String propertyName) {

        DynamicOptionsProperty dynamicOptionsProperty = (DynamicOptionsProperty) componentDefinitionRegistry
            .getActionProperty(componentName, componentVersion, actionName, propertyName);

        OptionsDataSource optionsDataSource = OptionalUtils.get(dynamicOptionsProperty.getOptionsDataSource());

        return (ComponentOptionsFunction) optionsDataSource.getOptions();
    }

    private ComponentPropertiesFunction getComponentPropertiesFunction(
        String componentName, int componentVersion, String actionName, String propertyName) {

        DynamicPropertiesProperty dynamicPropertiesProperty =
            (DynamicPropertiesProperty) componentDefinitionRegistry.getActionProperty(
                componentName, componentVersion, actionName, propertyName);

        PropertiesDataSource propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        return (ComponentPropertiesFunction) propertiesDataSource.getProperties();

    }

    private EditorDescriptionFunction getEditorDescriptionFunction(
        String componentName, int componentVersion, String actionName) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        com.bytechef.hermes.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(
                componentName, componentVersion, actionName);

        getActionDefinition(componentName, componentVersion, actionName);

        return OptionalUtils.mapOrElse(
            actionDefinition.getEditorDescriptionDataSource(),
            EditorDescriptionDataSource::getEditorDescription,
            (
                Connection connection,
                Map<String, ?> inputParameters) -> OptionalUtils.orElse(componentDefinition.getTitle(),
                    componentDefinition.getName()) + ": " +
                    OptionalUtils.orElse(actionDefinition.getTitle(), actionDefinition.getName()));
    }

    private OutputSchemaFunction
        getOutputSchemaFunction(String componentName, int componentVersion, String actionName) {
        com.bytechef.hermes.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(
                componentName, componentVersion, actionName);

        OutputSchemaDataSource outputSchemaDataSource = OptionalUtils.get(actionDefinition.getOutputSchemaDataSource());

        return outputSchemaDataSource.getOutputSchema();
    }

    private SampleOutputFunction
        getSampleOutputFunction(String componentName, int componentVersion, String actionName) {
        com.bytechef.hermes.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(
                componentName, componentVersion, actionName);

        SampleOutputDataSource sampleOutputDataSource = OptionalUtils.get(
            actionDefinition.getSampleOutputDataSource());

        return sampleOutputDataSource.getSampleOutput();
    }

    private com.bytechef.hermes.component.definition.ActionDefinition resolveActionDefinition(
        String componentName, int componentVersion, String actionName) {

        com.bytechef.hermes.component.definition.ActionDefinition actionDefinition;

        if (Objects.equals(actionName, CustomActionUtils.CUSTOM)) {
            ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
                componentName, componentVersion);

            actionDefinition = CustomActionUtils.getCustomActionDefinition(componentDefinition);
        } else {
            actionDefinition = componentDefinitionRegistry.getActionDefinition(
                componentName, componentVersion, actionName);
        }

        return actionDefinition;
    }
}
