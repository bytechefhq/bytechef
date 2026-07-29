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

package com.bytechef.embedded.integration;

import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.util.List;

/**
 * Entry point implemented by an embedded integration so that the platform can discover its
 * {@link IntegrationDefinition}.
 *
 * <p>
 * A handler supplies the integration's definition and exposes convenience accessors that delegate to it, giving the
 * platform a uniform way to read the integration's name, version, and the workflows it bundles.
 *
 * @author Ivica Cardic
 */
public interface IntegrationHandler {

    /**
     * Returns the definition that describes this integration's identity and the workflows it contains.
     *
     * @return the {@link IntegrationDefinition} for this integration
     */
    IntegrationDefinition getDefinition();

    /**
     * Returns the name of the integration, derived from the component name declared by its
     * {@link IntegrationDefinition}.
     *
     * @return the integration name
     */
    default String getName() {
        IntegrationDefinition integrationDefinition = getDefinition();

        return integrationDefinition.getComponentName();
    }

    /**
     * Returns the version of the integration, as declared by its {@link IntegrationDefinition}.
     *
     * @return the integration version
     */
    default String getVersion() {
        IntegrationDefinition integrationDefinition = getDefinition();

        return integrationDefinition.getVersion();
    }

    /**
     * Returns the workflows bundled by the integration, or an empty list if none are declared.
     *
     * @return the list of {@link WorkflowDefinition} instances belonging to the integration, never {@code null}
     */
    default List<WorkflowDefinition> getWorkflows() {
        IntegrationDefinition integrationDefinition = getDefinition();

        return integrationDefinition.getWorkflows()
            .orElse(List.of());
    }
}
