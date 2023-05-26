
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

import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinitionService {

    String executeEditorDescription(
        String actionName, String componentName, int componentVersion, Map<String, ?> actionParameters,
        String authorizationName, Map<String, ?> connectionParameters);

    List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, ?> actionParameters, String authorizationName, Map<String, ?> connectionParameters);

    List<OptionDTO> executeOptions(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, ?> actionParameters, String authorizationName, Map<String, ?> connectionParameters);

    List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String actionName, String componentName, int componentVersion, Map<String, ?> actionParameters,
        String authorizationName, Map<String, ?> connectionParameters);

    Object executeSampleOutput(
        String actionName, String componentName, int componentVersion, Map<String, ?> actionParameters,
        String authorizationName, Map<String, ?> connectionParameters);

    ActionDefinitionDTO getActionDefinition(
        String actionName, String componentName, int componentVersion);

    List<ActionDefinitionDTO> getActionDefinitions(String componentName, int componentVersion);
}
