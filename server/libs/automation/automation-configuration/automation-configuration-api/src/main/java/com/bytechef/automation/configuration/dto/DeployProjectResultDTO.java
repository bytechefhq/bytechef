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

package com.bytechef.automation.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * What {@code ProjectDeploymentFacade.deployProject} did, or why it wrote nothing. {@link #written()} is false when an
 * enabled workflow has an unresolved required connection slot or a missing required input; then
 * {@link #projectDeploymentId()} is the deployment that would have been updated, or null for a create.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record DeployProjectResultDTO(
    @Nullable Long projectDeploymentId, int projectVersion, List<DeployedWorkflow> workflows,
    List<RemovedWorkflow> removedWorkflows, List<UnresolvedConnection> unresolvedConnections,
    List<MissingInput> missingInputs) {

    public boolean written() {
        return unresolvedConnections.isEmpty() && missingInputs.isEmpty();
    }

    /**
     * One workflow of the deployed version. {@code workflowId} is that version's workflow id; {@code draftWorkflowId}
     * is the id of the same workflow in the project's DRAFT version, the one to edit next.
     */
    @SuppressFBWarnings("EI")
    public record DeployedWorkflow(
        String workflowUuid, String workflowId, String name, boolean enabled, @Nullable String draftWorkflowId,
        List<DeployedConnection> connections) {
    }

    public record DeployedConnection(String workflowNodeName, String key, long connectionId, String connectionName) {
    }

    public record RemovedWorkflow(String workflowUuid, String name) {
    }

    @SuppressFBWarnings("EI")
    public record UnresolvedConnection(
        String workflowUuid, String workflowName, String workflowNodeName, String key, String componentName,
        boolean required, List<Candidate> candidates) {
    }

    public record Candidate(long id, String name) {
    }

    public record MissingInput(String workflowUuid, String name) {
    }
}
