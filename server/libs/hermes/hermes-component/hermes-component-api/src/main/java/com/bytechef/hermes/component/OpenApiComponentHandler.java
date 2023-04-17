
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
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.definition.DefinitionDSL;

import java.util.List;

/**
 * The component handler implemented by components which are generated by the REST component generator.
 *
 * @author Ivica Cardic
 */
public interface OpenApiComponentHandler extends ComponentDefinitionFactory {

    enum PropertyType {
        BODY, PATH, HEADER, QUERY
    }

    default ComponentDSL.ModifiableActionDefinition[] modifyActions(
        ComponentDSL.ModifiableActionDefinition... actionDefinitions) {

        return actionDefinitions;
    }

    default List<ComponentDSL.ModifiableActionDefinition>[] modifyActions(
        List<ComponentDSL.ModifiableActionDefinition>... actionsList) {

        return actionsList;
    }

    default ComponentDSL.ModifiableConnectionDefinition modifyConnection(
        ComponentDSL.ModifiableConnectionDefinition connectionDefinition) {

        return connectionDefinition;
    }

    default ComponentDSL.ModifiableAuthorization modifyAuthorization(
        ComponentDSL.ModifiableAuthorization authorization) {

        return authorization;
    }

    default DefinitionDSL.ModifiableDisplay modifyDisplay(DefinitionDSL.ModifiableDisplay modifiableDisplay) {
        return modifiableDisplay;
    }

    default List<TriggerDefinition> getTriggers() {
        return List.of();
    }

    default Object postExecute(ActionDefinition actionDefinition, Object result) {
        return result;
    }
}
