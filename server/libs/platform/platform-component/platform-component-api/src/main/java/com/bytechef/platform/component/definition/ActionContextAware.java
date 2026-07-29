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
import java.util.Map;
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
     * Indicates whether the action is executing in an editor environment (e.g. the workflow editor test run backed by
     * {@code JobSyncExecutor}), as opposed to a production workflow run.
     *
     * @return {@code true} when running in the editor, {@code false} otherwise
     */
    boolean isEditorEnvironment();

    /**
     * Retrieves the suspend state if the action called {@link ActionContext#suspend(Suspend)} during execution.
     *
     * @return the {@link Suspend} instance if the action requested suspension, or {@code null} if no suspension was
     *         requested
     */
    @Nullable
    Suspend getSuspend();

    /**
     * Retrieves the job resume ID string that was generated during suspend processing.
     *
     * @return the job resume ID string, or null if not set
     */
    @Nullable
    String getJobResumeId();

    /**
     * Generates a resume URL for the current job. Creates a new job resume ID and returns the full URL that external
     * services can call to resume the suspended workflow.
     *
     * @return the resume URL, or null if publicUrl or jobId is not available
     */
    @Nullable
    String getResumeUrl();

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
     * Retrieves the parent task execution id of the current job, if the job runs as a sub-workflow of another job.
     * Returns {@code null} for top-level jobs (the common case) and for editor-environment / in-process invocations
     * with no persisted Atlas Job. The agent-tool sub-workflow bridge ({@code WorkflowCallWorkflowTool}) uses this to
     * fail fast when the agent itself runs as a sub-workflow, because a job with {@code parentTaskExecutionId != null}
     * cannot be resumed (see {@code JobServiceImpl.resumeToStatusStarted}).
     *
     * @return the parent task execution id, or {@code null} when the job is top-level or unavailable
     */
    @Nullable
    Long getParentTaskExecutionId();

    /**
     * Retrieves the parent Atlas Job's static metadata map. Phase 17b: surfaces the workflow-level metadata so actions
     * can read platform-injected, trigger-time {@code JobParameter} overrides stored under the reserved
     * {@code __jobParameters} key. The dataStream task action uses this to fold {@code datastream.mode} /
     * {@code datastream.since} entries into Spring Batch's {@code JobParameters} at perform-time.
     *
     * <p>
     * Returns an empty map (never null) when the parent job has no metadata, when the lookup is skipped (e.g.
     * editor-environment runs with no persisted job), or when the action has no associated {@code jobId}.
     * Implementations may load the job lazily — caching the map across calls within a single action invocation is
     * permitted but not required.
     * </p>
     */
    Map<String, Object> getJobMetadata();

    /**
     * Retrieves the platform type for the current context.
     *
     * @return the {@link PlatformType} if available,
     */
    @Nullable
    PlatformType getPlatformType();

    /**
     * Get the public URL for webhook callbacks and external service integrations.
     *
     * @return the public URL, or null if not configured
     */
    @Nullable
    String getPublicUrl();

    /**
     * Retrieves the identifier of the workflow.
     *
     * @return the workflow identifier as a string, or null if no workflow is associated.
     */
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
