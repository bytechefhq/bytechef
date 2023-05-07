
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.definition.Resources;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Used for specifying a component.
 *
 * @author Ivica Cardic
 */
@JsonDeserialize(as = ComponentDSL.ModifiableComponentDefinition.class)
public sealed interface ComponentDefinition permits ComponentDSL.ModifiableComponentDefinition {

    Optional<FilterCompatibleConnectionDefinitionsFunction> getFilterCompatibleConnectionDefinitions();

    Optional<List<? extends ActionDefinition>> getActions();

    Optional<String> getCategory();

    Optional<ConnectionDefinition> getConnection();

    Optional<Boolean> getCustomAction();

    Optional<Help> getCustomActionHelp();

    Optional<String> getDescription();

    String getIcon();

    Map<String, Object> getMetadata();

    String getName();

    NodeDescriptionFunction getNodeDescription();

    Optional<Resources> getResources();

    Optional<String[]> getTags();

    String getTitle();

    Optional<List<? extends TriggerDefinition>> getTriggers();

    int getVersion();

    /**
     *
     */
    @FunctionalInterface
    interface FilterCompatibleConnectionDefinitionsFunction {

        /**
         *
         * @param componentDefinition
         * @param connectionDefinitions
         * @return
         */
        List<ConnectionDefinition> apply(
            ComponentDefinition componentDefinition, List<ConnectionDefinition> connectionDefinitions);
    }
}
