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
    extends ClusterElementComponentDefinition, UnifiedApiComponentDefinition, WorkflowComponentDefinition {

    /**
     * Returns the categories under which this component is classified.
     *
     * @return an {@code Optional} containing the list of component categories if defined, or an empty {@code Optional}
     *         otherwise
     */
    Optional<List<ComponentCategory>> getComponentCategories();

    /**
     * Returns the connection definition that describes how this component authenticates with the external service.
     *
     * @return an {@code Optional} containing the connection definition if defined, or an empty {@code Optional}
     *         otherwise
     */
    Optional<ConnectionDefinition> getConnection();

    /**
     * Returns the human-readable description of the component, explaining what it does.
     *
     * @return an {@code Optional} containing the description if defined, or an empty {@code Optional} otherwise
     */
    Optional<String> getDescription();

    /**
     * Returns the icon representing the component in the user interface, typically as SVG markup or a reference to an
     * icon resource.
     *
     * @return an {@code Optional} containing the icon if defined, or an empty {@code Optional} otherwise
     */
    Optional<String> getIcon();

    /**
     * Returns optional, implementation-specific metadata associated with the component.
     *
     * @return an {@code Optional} containing the metadata map if defined, or an empty {@code Optional} otherwise
     */
    Optional<Map<String, Object>> getMetadata();

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
    Optional<Resources> getResources();

    /**
     * TODO
     *
     * @return
     */
    Optional<List<String>> getTags();

    /**
     * Returns the human-readable title of the component displayed in the user interface.
     *
     * @return an {@code Optional} containing the title if defined, or an empty {@code Optional} otherwise
     */
    Optional<String> getTitle();

    /**
     * Returns the version of the component, used to distinguish successive revisions of its definition.
     *
     * @return the component version
     */
    int getVersion();
}
