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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.TriggerContext;
import com.bytechef.platform.constant.PlatformType;
import org.jspecify.annotations.Nullable;

/**
 * Represents a specialized extension of the {@link TriggerContext} interface that provides additional context for
 * triggers, including identifiers and metadata for environment, job principal, and workflow. <br>
 * Implementations of this interface should provide access to context-specific details necessary for trigger execution
 * within workflows or automation processes.
 */
public interface TriggerContextAware extends TriggerContext {

    /**
     * Retrieves the unique identifier of the environment associated with the current trigger context.
     *
     * @return the environment ID if available, or {@code null} if the environment ID is not set or applicable.
     */
    @Nullable
    Long getEnvironmentId();

    /**
     * Retrieves the unique identifier of the job principal associated with the current trigger context.
     *
     * @return the job principal ID if available, or {@code null} if the job principal ID is not set or applicable.
     */
    @Nullable
    Long getJobPrincipalId();

    /**
     * Retrieves the name of the trigger associated with the current context.
     *
     * @return the trigger name, or {@code null} if no trigger name is assigned or applicable.
     */
    String getTriggerName();

    /**
     * Retrieves the platform type associated with the current trigger context.
     *
     * @return the {@link PlatformType} representing the platform configuration, indicating whether the context is for
     *         automation or embedded use cases.
     */
    PlatformType getType();

    /**
     * Retrieves the unique identifier of the workflow associated with the current trigger context.
     *
     * @return the workflow UUID, or {@code null} if the workflow UUID is not set or applicable.
     */
    String getWorkflowUuid();
}
