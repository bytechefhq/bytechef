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

package com.bytechef.ee.platform.apiconnector.handler.reader;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.definition.BaseProperty;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.apiconnector.file.storage.ApiConnectorFileStorage;
import com.bytechef.platform.component.definition.ActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ComponentDefinitionWrapper;
import com.bytechef.platform.component.util.OpenApiClientUtils;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ComponentDefinitionReader {

    private static final Function<ActionDefinition, ActionDefinition.PerformFunction> PERFORM_FUNCTION_FUNCTION =
        actionDefinition -> (inputParameters, connectionParameters, context) -> OpenApiClientUtils.execute(
            inputParameters, OptionalUtils.orElse(actionDefinition.getProperties(), List.of()),
            OptionalUtils.orElse(actionDefinition.getOutputDefinition(), null),
            OptionalUtils.orElse(actionDefinition.getMetadata(), Map.of()),
            OptionalUtils.orElse(actionDefinition.getProcessErrorResponse(), null), context);

    private final ApiConnectorFileStorage apiConnectorFileStorage;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public ComponentDefinitionReader(ApiConnectorFileStorage apiConnectorFileStorage, ObjectMapper objectMapper) {
        this.apiConnectorFileStorage = apiConnectorFileStorage;
        this.objectMapper = objectMapper.copy()
            .addMixIn(Property.class, PropertyMixIn.class)
            .addMixIn(BaseProperty.BaseValueProperty.class, PropertyMixIn.class);

        registerAbstractTypeMapping(ActionDefinition.class, ModifiableActionDefinition.class);
        registerAbstractTypeMapping(ConnectionDefinition.class, ModifiableConnectionDefinition.class);
        registerAbstractTypeMapping(ClusterElementDefinition.class, ModifiableClusterElementDefinition.class);
        registerAbstractTypeMapping(TriggerDefinition.class, ModifiableTriggerDefinition.class);
    }

    public ComponentDefinitionWrapper readComponentDefinition(ApiConnector apiConnector) {
        ComponentDsl.ModifiableComponentDefinition componentDefinition;

        try {
            componentDefinition = objectMapper.readValue(
                apiConnectorFileStorage.readApiConnectorDefinition(apiConnector.getDefinition()),
                ComponentDsl.ModifiableComponentDefinition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }

        return new ComponentDefinitionWrapper(
            componentDefinition,
            componentDefinition.getActions()
                .map(actionDefinitions -> actionDefinitions.stream()
                    .map(actionDefinition -> (ActionDefinition) new ActionDefinitionWrapper(
                        actionDefinition, PERFORM_FUNCTION_FUNCTION.apply(actionDefinition)))
                    .toList())
                .orElse(List.of()));
    }

    private <T> void registerAbstractTypeMapping(Class<T> abstractType, Class<? extends T> concreteType) {
        SimpleModule simpleModule = new SimpleModule();

        simpleModule.addAbstractTypeMapping(abstractType, concreteType);

        objectMapper.registerModule(simpleModule);
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableArrayProperty.class, name = "ARRAY"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableBooleanProperty.class, name = "BOOLEAN"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableDateProperty.class, name = "DATE"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableDateTimeProperty.class, name = "DATE_TIME"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableDynamicPropertiesProperty.class, name = "DYNAMIC_PROPERTIES"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableFileEntryProperty.class, name = "FILE_ENTRY"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableIntegerProperty.class, name = "INTEGER"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableNumberProperty.class, name = "NUMBER"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableNullProperty.class, name = "NULL"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableObjectProperty.class, name = "OBJECT"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableStringProperty.class, name = "STRING"),
        @JsonSubTypes.Type(value = ComponentDsl.ModifiableTimeProperty.class, name = "TIME"),
    })
    @SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
    public abstract static class PropertyMixIn {
    }
}
