
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

package com.bytechef.hermes.component.registry.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentOptionsFunction;
import com.bytechef.hermes.component.definition.ComponentPropertiesFunction;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource.EditorDescriptionFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.SampleOutputDataSource;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.SampleOutputFunction;
import com.bytechef.hermes.component.definition.factory.ContextFactory;
import com.bytechef.hermes.component.registry.ComponentDefinitionRegistry;
import com.bytechef.hermes.component.definition.ParameterMapImpl;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.definition.DynamicOptionsProperty;
import com.bytechef.hermes.definition.OptionsDataSource;
import com.bytechef.hermes.definition.PropertiesDataSource;
import com.bytechef.hermes.definition.Property.DynamicPropertiesProperty;
import com.bytechef.hermes.component.registry.domain.ActionDefinition;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.registry.domain.Property;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.registry.domain.ValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Service("actionDefinitionService")
public class ActionDefinitionServiceImpl implements ActionDefinitionService, RemoteActionDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ContextFactory contextFactory;

    @SuppressFBWarnings("EI2")
    public ActionDefinitionServiceImpl(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextFactory = contextFactory;
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, Connection connection) {

        ComponentPropertiesFunction propertiesFunction = getComponentPropertiesFunction(
            componentName, componentVersion, actionName, propertyName);

        List<? extends com.bytechef.hermes.definition.Property.ValueProperty<?>> valueProperties =
            propertiesFunction.apply(
                new ParameterMapImpl(inputParameters),
                connection == null ? null : new ParameterMapImpl(connection.getParameters()),
                contextFactory.createActionContext(connection));

        return valueProperties.stream()
            .map(valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty))
            .toList();
    }

    @Override
    public String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters,
        Connection connection) {

        EditorDescriptionFunction editorDescriptionFunction = getEditorDescriptionFunction(
            componentName, componentVersion, actionName);

        return editorDescriptionFunction.apply(
            new ParameterMapImpl(inputParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()));
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, String searchText, Connection connection) {

        ComponentOptionsFunction optionsFunction = getComponentOptionsFunction(
            componentName, componentVersion, actionName, propertyName);

        List<com.bytechef.hermes.definition.Option<?>> options = optionsFunction.apply(
            new ParameterMapImpl(inputParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()),
            searchText, contextFactory.createActionContext(connection));

        return options.stream()
            .map(Option::new)
            .toList();
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters,
        Connection connection) {

        OutputSchemaFunction outputSchemaFunction = getOutputSchemaFunction(
            componentName, componentVersion, actionName);

        return Property.toProperty(
            outputSchemaFunction.apply(
                new ParameterMapImpl(inputParameters),
                connection == null ? null : new ParameterMapImpl(connection.getParameters())));
    }

    @Override
    public Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, long taskExecutionId,
        @NonNull Map<String, ?> inputParameters, Connection connection) {

        com.bytechef.hermes.component.definition.ActionDefinition actionDefinition =
            resolveActionDefinition(componentName, componentVersion, actionName);

        return OptionalUtils.mapOrElse(
            actionDefinition.getPerform(), performFunction -> performFunction.apply(
                new ParameterMapImpl(inputParameters),
                connection == null ? null : new ParameterMapImpl(connection.getParameters()),
                contextFactory.createActionContext(connection, taskExecutionId)),
            null);
    }

    @Override
    public Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> actionParameters, Connection connection) {

        SampleOutputFunction sampleOutputFunction = getSampleOutputFunction(
            componentName, componentVersion, actionName);

        return sampleOutputFunction.apply(
            new ParameterMapImpl(actionParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()));
    }

    @Override
    public ActionDefinition
        getActionDefinition(@NonNull String componentName, int componentVersion, @NonNull String actionName) {
        return new ActionDefinition(resolveActionDefinition(componentName, componentVersion, actionName));
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(@NonNull String componentName, int componentVersion) {
        return componentDefinitionRegistry.getActionDefinitions(componentName, componentVersion)
            .stream()
            .map(ActionDefinition::new)
            .toList();
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(@NonNull List<ComponentOperation> componentOperations) {
        List<ActionDefinition> actionDefinitions;

        if (componentOperations.isEmpty()) {
            actionDefinitions = CollectionUtils.map(
                componentDefinitionRegistry.getActionDefinitions(), ActionDefinition::new);
        } else {
            actionDefinitions = CollectionUtils.map(
                componentOperations,
                componentOperation -> getActionDefinition(
                    componentOperation.componentName(), componentOperation.componentVersion(),
                    componentOperation.operationName()));
        }

        return actionDefinitions;
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
            (inputParameters, connectionParameters) -> OptionalUtils.orElse(componentDefinition.getTitle(),
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

    private SampleOutputFunction getSampleOutputFunction(
        String componentName, int componentVersion, String actionName) {

        com.bytechef.hermes.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(
                componentName, componentVersion, actionName);

        SampleOutputDataSource sampleOutputDataSource = OptionalUtils.get(
            actionDefinition.getSampleOutputDataSource());

        return sampleOutputDataSource.getSampleOutput();
    }

    private com.bytechef.hermes.component.definition.ActionDefinition resolveActionDefinition(
        String componentName, int componentVersion, String actionName) {

        return componentDefinitionRegistry.getActionDefinition(
            componentName, componentVersion, actionName);
    }
}
