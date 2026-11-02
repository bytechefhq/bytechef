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

package com.bytechef.component.definition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Used for specifying a component.
 *
 * @author Ivica Cardic
 */
public interface ComponentDefinition
    extends AgentChannelComponentDefinition, ClusterElementComponentDefinition, UnifiedApiComponentDefinition,
    WorkflowComponentDefinition {

    int VERSION_1 = 1;

    /**
     * Returns the categories under which this component is classified.
     *
     * @return the list of component categories, or an empty list if none are defined
     */
    default List<ComponentCategory> getComponentCategories() {
        return List.of();
    }

    /**
     * Returns the connection definition that describes how this component authenticates with the external service.
     *
     * @return an {@code Optional} containing the connection definition if defined, or an empty {@code Optional}
     *         otherwise
     */
    default Optional<ConnectionDefinition> getConnection() {
        return Optional.empty();
    }

    /**
     * Returns the human-readable description of the component, explaining what it does.
     *
     * @return an {@code Optional} containing the description if defined, or an empty {@code Optional} otherwise
     */
    default Optional<String> getDescription() {
        return Optional.empty();
    }

    /**
     * Returns the icon representing the component in the user interface, typically as SVG markup or a reference to an
     * icon resource.
     *
     * @return an {@code Optional} containing the icon if defined, or an empty {@code Optional} otherwise
     */
    default Optional<String> getIcon() {
        return Optional.empty();
    }

    /**
     * Returns optional, implementation-specific metadata associated with the component.
     *
     * @return the metadata map, or an empty map if none is defined
     */
    default Map<String, Object> getMetadata() {
        return Map.of();
    }

    /**
     * Returns the unique name that identifies this component.
     *
     * @return the component name
     */
    String getName();

    /**
     * TODO
     *
     * @return
     */
    default Optional<Resources> getResources() {
        return Optional.empty();
    }

    /**
     * TODO
     *
     * @return
     */
    default List<String> getTags() {
        return List.of();
    }

    /**
     * Returns the human-readable title of the component displayed in the user interface.
     *
     * @return an {@code Optional} containing the title if defined, or an empty {@code Optional} otherwise
     */
    default Optional<String> getTitle() {
        return Optional.empty();
    }

    /**
     * Returns the version of the component, used to distinguish successive revisions of its definition.
     *
     * @return the component version
     */
    default int getVersion() {
        return VERSION_1;
    }
}
