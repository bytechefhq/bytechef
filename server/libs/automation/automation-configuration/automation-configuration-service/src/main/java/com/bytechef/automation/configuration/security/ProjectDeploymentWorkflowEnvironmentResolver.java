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

package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Answers the environment of the deployment owning a project-deployment-workflow row, so that a by-id check on the
 * {@code 'ProjectDeploymentWorkflow'} token is judged against the role the caller holds THERE. Without it the token
 * would keep the environment-unaware check, which is what
 * {@link com.bytechef.automation.configuration.service.PermissionService#hasResourceScope} falls back to — an editor in
 * Development would pass a check on a Production deployment's workflow, which is precisely the escalation the
 * environment half of the model exists to stop.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectDeploymentWorkflowEnvironmentResolver implements ResourceEnvironmentResolver {

    private final ProjectDeploymentRepository projectDeploymentRepository;
    private final ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentWorkflowEnvironmentResolver(
        ProjectDeploymentRepository projectDeploymentRepository,
        ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository) {

        this.projectDeploymentRepository = projectDeploymentRepository;
        this.projectDeploymentWorkflowRepository = projectDeploymentWorkflowRepository;
    }

    @Override
    public String resourceType() {
        return "ProjectDeploymentWorkflow";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return projectDeploymentWorkflowRepository.findById(number.longValue())
            .map(ProjectDeploymentWorkflow::getProjectDeploymentId)
            .flatMap(projectDeploymentRepository::findById)
            .map(ProjectDeployment::getEnvironment);
    }
}
