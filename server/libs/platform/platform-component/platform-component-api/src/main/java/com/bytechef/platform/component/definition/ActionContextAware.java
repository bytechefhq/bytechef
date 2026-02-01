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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.constant.PlatformType;
import org.jspecify.annotations.Nullable;

/**
 * A context-aware interface that extends both {@link ActionContext} and {@link JobContextAware}. This interface
 * provides additional functionality for handling contextual information related to actions, job principals,
 * environments, workflow identifiers, platform types, and public URLs within a broader execution or integration
 * platform.
 *
 * @author Ivica Cardic
 */
public interface ActionContextAware extends ActionContext, JobContextAware {

    /**
     * Retrieves the name of the action associated with the current context.
     *
     * @return the action name as a string, or null if the action name is not available.
     */
    String getActionName();

    /**
     * Retrieves the unique identifier of the environment.
     *
     * @return the environment ID as a Long if it exists, or null if no ID is associated.
     */
    @Nullable
    Long getEnvironmentId();

    /**
     * Retrieves the unique identifier associated with the job principal.
     *
     * @return the job principal ID as a {@link Long}, or {@code null} if no job principal ID is set.
     */
    @Nullable
    Long getJobPrincipalId();

    /**
     * Retrieves the identifier of the workflow associated with the job principal.
     *
     * @return the workflow identifier as a {@link Long}, or {@code null} if no workflow is associated with the job
     *         principal.
     */
    @Nullable
    Long getJobPrincipalWorkflowId();

    /**
     * Retrieves the unique identifier for a job, if available.
     *
     * @return the job ID as a {@link Long}, or {@code null} if the job ID is not set.
     */
    @Nullable
    Long getJobId();

    /**
     * Retrieves the platform type for the current context.
     *
     * @return the {@link PlatformType} if available,
     */
    @Nullable
    PlatformType getPlatformType();

    @Nullable
    String getWorkflowId();

    /**
     * Converts the given component details and cluster element information to a {@link ClusterElementContext}.
     *
     * @param componentName       the name of the component, used to identify the component in the cluster context
     * @param componentVersion    the version of the component, used to distinguish between component iterations
     * @param clusterElementName  the name of the cluster element being processed
     * @param componentConnection the connection details associated with the component, or null if no connection exists
     * @return a {@link ClusterElementContext} instance representing the cluster element in the context of the specified
     *         component and connection
     */
    ClusterElementContext toClusterElementContext(
        String componentName, int componentVersion, String clusterElementName,
        @Nullable ComponentConnection componentConnection);
}
