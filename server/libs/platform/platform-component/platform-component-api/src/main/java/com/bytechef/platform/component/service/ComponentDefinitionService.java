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

package com.bytechef.platform.component.service;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ComponentDefinitionService {

    Optional<ComponentDefinition> fetchComponentDefinition(String name, @Nullable Integer version);

    ComponentDefinition getComponentDefinition(String name, @Nullable Integer version);

    List<ComponentDefinition> getComponentDefinitions();

    List<ComponentDefinition> getComponentDefinitions(
        Boolean actionDefinitions, Boolean connectionDefinitions, Boolean triggerDefinitions,
        @Nullable List<String> include, PlatformType platformType);

    List<ComponentDefinition> getComponentDefinitions(String query, PlatformType platformType);

    List<ComponentDefinition> getComponentDefinitionVersions(String name);

    ComponentDefinition getConnectionComponentDefinition(String name, int connectionVersion);

    boolean hasComponentDefinition(String name, @Nullable Integer version);
}
