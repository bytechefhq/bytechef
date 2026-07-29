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

package com.bytechef.automation.ai.a2a.facade;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import java.util.List;

/**
 * Facade for A2A project operations that span multiple services — provisioning the {@code ProjectDeployment} and
 * {@code ProjectDeploymentWorkflow} rows an {@link A2aProject} binds to when workflows are exposed as A2A skills.
 *
 * @author Ivica Cardic
 */
public interface A2aProjectFacade {

    /**
     * Creates an A2A project on the given server: provisions a {@code ProjectDeployment} for the project version, then
     * a {@code ProjectDeploymentWorkflow} + {@code A2aProjectWorkflow} for each selected workflow (each exposed as an
     * A2A skill).
     *
     * @param a2aServerId         the A2A server the project is exposed under
     * @param projectId           the project whose workflows are exposed
     * @param projectVersion      the project version
     * @param selectedWorkflowIds the workflow ids to expose as skills
     * @return the created A2A project
     */
    A2aProject createA2aProject(long a2aServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds);

    /**
     * Deletes an A2A project and the deployment + deployment-workflow rows it provisioned.
     *
     * @param a2aProjectId the A2A project id
     */
    void deleteA2aProject(long a2aProjectId);

    /**
     * Reconciles the exposed workflow set of an A2A project against {@code selectedWorkflowIds}: newly selected
     * workflows are added, deselected ones removed, unchanged ones preserved.
     *
     * @param a2aProjectId        the A2A project id
     * @param selectedWorkflowIds the new set of exposed workflow ids
     * @return the updated A2A project
     */
    A2aProject updateA2aProject(long a2aProjectId, List<String> selectedWorkflowIds);
}
