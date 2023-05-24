
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

package com.bytechef.hermes.component;

import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty;
import com.bytechef.hermes.definition.Property.InputProperty;

import java.util.Arrays;
import java.util.List;

/**
 * The component handler implemented by components which are generated by the REST component generator.
 *
 * @author Ivica Cardic
 */
public interface OpenApiComponentHandler extends ComponentDefinitionFactory {

    /**
     *
     */
    enum PropertyType {
        BODY, PATH, HEADER, QUERY
    }

    /**
     *
     * @param actionDefinitions
     * @return
     */
    default List<ActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        return Arrays.stream(actionDefinitions)
            .map(this::modifyAction)
            .map(actionDefinition -> {
                InputProperty[] properties = actionDefinition.getProperties()
                    .orElse(List.of())
                    .stream()
                    .map(property -> (InputProperty) modifyProperty(
                        actionDefinition, (ModifiableProperty<?, ?>) property))
                    .toArray(InputProperty[]::new);

                return (ActionDefinition) actionDefinition.properties(properties);
            })
            .toList();
    }

    /**
     *
     * @return
     */
    default List<TriggerDefinition> getTriggers() {
        return List.of();
    }

    /**
     *
     * @param actionDefinition
     * @return
     */
    default ModifiableActionDefinition modifyAction(ModifiableActionDefinition actionDefinition) {
        return actionDefinition;
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
     * @param connectionDefinition
     * @return
     */
    default ModifiableConnectionDefinition modifyConnection(ModifiableConnectionDefinition connectionDefinition) {
        return connectionDefinition;
    }

    /**
     *
     * @param property
     * @return
     */
    default ModifiableProperty<?, ?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?, ?> property) {

        return property;
    }

    /**
     *
     * @param actionDefinition
     * @param result
     * @return
     */
    default Object postExecute(ActionDefinition actionDefinition, Object result) {
        return result;
    }
}
