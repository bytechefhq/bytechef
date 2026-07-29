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

package com.bytechef.embedded.integration.definition;

import com.bytechef.workflow.definition.WorkflowDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Defines the metadata and contents of an embedded integration.
 *
 * <p>
 * An integration ties a set of {@link WorkflowDefinition workflows} to a specific ByteChef component (identified by its
 * {@link #getComponentName() name} and {@link #getComponentVersion() version}) and carries the metadata needed to
 * publish it as part of the embedded iPaaS, such as its category, description, tags, versioning, and whether it may be
 * configured as multiple instances.
 *
 * @author Ivica Cardic
 */
public interface IntegrationDefinition {

    /**
     * Returns the category the integration belongs to, if one has been assigned.
     *
     * @return an {@link Optional} containing the integration category, or an empty {@link Optional} if none is set
     */
    Optional<String> getCategory();

    /**
     * Returns the name of the ByteChef component this integration is built around.
     *
     * @return the component name
     */
    String getComponentName();

    /**
     * Returns the version of the ByteChef component this integration is built around.
     *
     * @return the component version
     */
    int getComponentVersion();

    /**
     * Returns the human-readable description of the integration, if one has been provided.
     *
     * @return an {@link Optional} containing the integration description, or an empty {@link Optional} if none is set
     */
    Optional<String> getDescription();

    /**
     * Returns whether the integration may be configured with multiple concurrent instances.
     *
     * @return {@code true} if multiple instances of this integration are allowed, {@code false} otherwise
     */
    boolean isMultipleInstances();

    /**
     * Returns the tags associated with the integration, if any have been assigned.
     *
     * @return an {@link Optional} containing the list of tags, or an empty {@link Optional} if none are set
     */
    Optional<List<String>> getTags();

    /**
     * Returns the version of the integration.
     *
     * @return the integration version
     */
    String getVersion();

    /**
     * Returns the workflows bundled by the integration, if any have been declared.
     *
     * @return an {@link Optional} containing the list of {@link WorkflowDefinition} instances, or an empty
     *         {@link Optional} if none are declared
     */
    Optional<List<WorkflowDefinition>> getWorkflows();
}
