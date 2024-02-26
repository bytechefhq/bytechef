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

package com.bytechef.platform.component.registry.service;

import com.bytechef.platform.component.definition.DataStreamComponentDefinition.ComponentType;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ComponentDefinitionService {

    ComponentDefinition getComponentDefinition(String name, Integer version);

    List<ComponentDefinition> getComponentDefinitions();

    List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions, List<String> include);

    List<ComponentDefinition> getComponentDefinitionVersions(String name);

    List<ComponentDefinition> getDataStreamComponentDefinitions(ComponentType componentType);
}
