/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Property;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * The component handler implemented by components which are generated by the REST component generator.
 *
 * @author Ivica Cardic
 */
public interface OpenAPIComponentHandler extends ComponentHandler {

    /**
     *
     */
    enum PropertyType {
        BODY, PATH, HEADER, QUERY
    }

    /**
     *
     * @return
     */
    default List<? extends ModifiableActionDefinition> getCustomActions() {
        return List.of();
    }

    /**
     *
     * @return
     */
    default List<ModifiableTriggerDefinition> getTriggers() {
        return List.of();
    }

    /**
     *
     * @param actionDefinitions
     * @return
     */
    default List<? extends ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        return Stream.concat(Arrays.stream(actionDefinitions), getCustomActions().stream())
            .map(this::modifyAction)
            .map(actionDefinition -> {
                Property[] properties = actionDefinition.getProperties()
                    .orElse(List.of())
                    .stream()
                    .map(property -> (Property) modifyProperty(
                        actionDefinition, (ModifiableProperty<?>) property))
                    .toArray(Property[]::new);

                return actionDefinition.properties(properties);
            })
            .toList();
    }

    /**
     *
     * @param modifiableActionDefinition
     * @return
     */
    default ModifiableActionDefinition modifyAction(ModifiableActionDefinition modifiableActionDefinition) {
        return modifiableActionDefinition;
    }

    /**
     *
     * @param modifiableComponentDefinition
     * @return
     */
    default ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition;
    }

    /**
     *
     * @param modifiableConnectionDefinition
     * @return
     */
    default ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition;
    }

    /**
     *
     * @param modifiableProperty
     * @return
     */
    default ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        return modifiableProperty;
    }

    /**
     *
     * @param actionName
     * @param response
     * @return
     */
    default Http.Response postExecute(String actionName, Http.Response response) {
        return response;
    }
}
