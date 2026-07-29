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

package com.bytechef.automation.project;

import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.util.List;

/**
 * Entry point implemented by an automation project so that the platform can discover its {@link ProjectDefinition}.
 *
 * <p>
 * A handler supplies the project's definition and exposes convenience accessors that delegate to it, giving the
 * platform a uniform way to read the project's name, version, and the workflows it bundles.
 *
 * @author Ivica Cardic
 */
public interface ProjectHandler {

    /**
     * Returns the definition that describes this project's identity and the workflows it contains.
     *
     * @return the {@link ProjectDefinition} for this project
     */
    ProjectDefinition getDefinition();

    /**
     * Returns the name of the project, as declared by its {@link ProjectDefinition}.
     *
     * @return the project name
     */
    default String getName() {
        ProjectDefinition projectDefinition = getDefinition();

        return projectDefinition.getName();
    }

    /**
     * Returns the version of the project, as declared by its {@link ProjectDefinition}.
     *
     * @return the project version
     */
    default String getVersion() {
        ProjectDefinition projectDefinition = getDefinition();

        return projectDefinition.getVersion();
    }

    /**
     * Returns the workflows bundled by the project, as declared by its {@link ProjectDefinition}.
     *
     * @return the list of {@link WorkflowDefinition} instances belonging to the project
     */
    default List<WorkflowDefinition> getWorkflows() {
        ProjectDefinition projectDefinition = getDefinition();

        return projectDefinition.getWorkflows();
    }
}
