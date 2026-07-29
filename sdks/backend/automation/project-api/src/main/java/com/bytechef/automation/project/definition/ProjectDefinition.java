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

package com.bytechef.automation.project.definition;

import com.bytechef.workflow.definition.WorkflowDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Defines the metadata and contents of an automation project.
 *
 * <p>
 * A project groups together a set of {@link WorkflowDefinition workflows} under a shared identity (name and version)
 * along with optional descriptive metadata such as a category, description, and tags. It is typically built through the
 * SDK's fluent DSL and surfaced to the platform via a {@code ProjectHandler}.
 *
 * @author Ivica Cardic
 */
public interface ProjectDefinition {

    /**
     * Returns the category the project belongs to, if one has been assigned.
     *
     * @return an {@link Optional} containing the project category, or an empty {@link Optional} if none is set
     */
    Optional<String> getCategory();

    /**
     * Returns the human-readable description of the project, if one has been provided.
     *
     * @return an {@link Optional} containing the project description, or an empty {@link Optional} if none is set
     */
    Optional<String> getDescription();

    /**
     * Returns the name that uniquely identifies the project.
     *
     * @return the project name
     */
    String getName();

    /**
     * Returns the workflows bundled by the project.
     *
     * @return the list of {@link WorkflowDefinition} instances belonging to the project
     */
    List<WorkflowDefinition> getWorkflows();

    /**
     * Returns the tags associated with the project, if any have been assigned.
     *
     * @return an {@link Optional} containing the list of tags, or an empty {@link Optional} if none are set
     */
    Optional<List<String>> getTags();

    /**
     * Returns the version of the project.
     *
     * @return the project version
     */
    String getVersion();
}
