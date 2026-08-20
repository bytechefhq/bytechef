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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ProjectDeploymentFacade {

    long createProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO);

    long createProjectDeployment(
        ProjectDeployment projectDeployment, String workflowId, List<ProjectDeploymentWorkflowConnection> connections);

    long createProjectDeployment(
        ProjectDeployment projectDeployment, List<ProjectDeploymentWorkflow> projectDeploymentWorkflows,
        List<Tag> tags);

    long createProjectDeploymentWorkflowJob(Long id, String workflowId);

    void deleteProjectDeployment(long id);

    void enableProjectDeployment(long id, boolean enable);

    void enableProjectDeploymentWorkflow(long projectDeploymentId, String workflowId, boolean enable);

    void enableProjectDeploymentWorkflow(long projectId, String workflowId, boolean enable, Environment environment);

    ProjectDeploymentDTO getProjectDeployment(long id);

    /**
     * One deployed workflow, keyed by the opaque workflow-execution id the chat and webhook surfaces carry.
     *
     * <p>
     * Decoded rather than raw, so that the gate on the implementation can read the deployment out of it: Spring
     * evaluates {@code @PreAuthorize} against the arguments of the method it enters, so an undecoded string could not
     * be gated on the deployment it names. The gate is a conjunction rather than a single scope; see the implementation
     * for which two and why.
     */
    ProjectDeploymentWorkflow getProjectDeploymentWorkflow(WorkflowExecutionId workflowExecutionId);

    List<Tag> getProjectDeploymentTags(long workspaceId);

    /**
     * The enabled hosted-chat workflows of the workspace the caller may see, flattened into one row per workflow.
     *
     * <p>
     * Purpose-built for the workflow chat sidebar and the AI Hub launcher: it batches the service calls and memoizes
     * the trigger-definition lookups a per-row read would repeat. The gate on the implementation and the batched
     * project-visibility filter inside it are the two halves the by-id reads these rows lead to already compose &mdash;
     * a launcher must not name a project or label a workflow that the caller would be denied on opening.
     */
    List<ChatWorkflow> getWorkspaceChatWorkflows(long workspaceId, long environmentId);

    /**
     * The deployment rows of one workspace the caller may see, as domain objects.
     *
     * <p>
     * The entity-returning sibling of the DTO listing below, for the GraphQL surface: its {@code ProjectDeployment}
     * type is assembled by field resolvers that take the domain object, and the DTO drops fields
     * ({@code createdBy}/{@code createdDate}/{@code lastModified*}) that type exposes. Both apply the same gate and the
     * same two filters — feature-owned system projects, then project visibility.
     */
    List<ProjectDeployment> getWorkspaceProjectDeployments(
        long workspaceId, long environmentId, Long projectId, Long tagId);

    List<ProjectDeploymentDTO> getWorkspaceProjectDeployments(
        long id, Long environmentId, Long projectId, Long tagId, boolean includeAllFields);

    void updateProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO);

    void updateProjectDeployment(
        long projectId, int projectVersion, String workflowUuid,
        List<ProjectDeploymentWorkflowConnection> connections, Long environmentId);

    void updateProjectDeployment(
        ProjectDeployment projectDeployment, List<ProjectDeploymentWorkflow> projectDeploymentWorkflows,
        List<Tag> tags);

    void updateProjectDeploymentTags(long id, List<Tag> tags);

    void updateProjectDeploymentWorkflow(ProjectDeploymentWorkflow projectDeploymentWorkflow);

    /**
     * One row of {@link #getWorkspaceChatWorkflows(long, long)}: a purpose-built projection rather than a domain type,
     * carrying exactly the fields a chat launcher renders plus the ids it needs to open the workflow without a second
     * round-trip.
     */
    record ChatWorkflow(
        long projectDeploymentId, long projectId, String projectName, long projectWorkflowId,
        String workflowExecutionId, String workflowId, String workflowLabel) {
    }
}
