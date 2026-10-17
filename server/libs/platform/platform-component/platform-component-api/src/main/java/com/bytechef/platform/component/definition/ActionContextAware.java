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
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
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

    Duration DEFAULT_APPROVAL_EXPIRY = Duration.ofDays(60);

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
     * Mints the resume and form links for an approval request on the current job.
     *
     * @param expiresAt when the approval request lapses
     * @return the links, or null if publicUrl or jobId is not available
     */
    @Nullable
    ApprovalLinks getApprovalLinks(Instant expiresAt);

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

    record ApprovalLinks(
        String publicUrl, String resumeId, String resumeUrl, String formUrl, @Nullable String shortResumeId) {

        public ApprovalLinks {
            Objects.requireNonNull(publicUrl, "publicUrl");
            Objects.requireNonNull(resumeId, "resumeId");
            Objects.requireNonNull(resumeUrl, "resumeUrl");
            Objects.requireNonNull(formUrl, "formUrl");
        }

        public ApprovalLinks(String publicUrl, String resumeId, String resumeUrl, String formUrl) {
            this(publicUrl, resumeId, resumeUrl, formUrl, null);
        }
    }
}
