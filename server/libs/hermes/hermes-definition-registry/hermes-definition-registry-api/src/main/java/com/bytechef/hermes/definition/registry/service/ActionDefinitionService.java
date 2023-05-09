
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

import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinitionService {

    String executeEditorDescription(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        String authorizationName, Map<String, Object> connectionParameters);

    List<Option<?>> executeOptions(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> actionParameters, String authorizationName, Map<String, Object> connectionParameters);

    List<? extends Property<?>> executeOutputSchema(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        String authorizationName, Map<String, Object> connectionParameters);

    List<? extends Property<?>> executeDynamicProperties(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> actionParameters, String authorizationName, Map<String, Object> connectionParameters);

    Object executeSampleOutput(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        String authorizationName, Map<String, Object> connectionParameters);

    ActionDefinitionDTO getComponentActionDefinition(
        String actionName, String componentName, int componentVersion);

    List<ActionDefinitionDTO> getComponentActionDefinitions(String componentName, int componentVersion);
}
