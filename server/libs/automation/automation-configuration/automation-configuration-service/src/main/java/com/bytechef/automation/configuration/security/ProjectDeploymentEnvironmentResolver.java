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
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectDeploymentEnvironmentResolver implements ResourceEnvironmentResolver {

    private final ProjectDeploymentRepository projectDeploymentRepository;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentEnvironmentResolver(ProjectDeploymentRepository projectDeploymentRepository) {
        this.projectDeploymentRepository = projectDeploymentRepository;
    }

    @Override
    public String resourceType() {
        return "ProjectDeployment";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return projectDeploymentRepository.findById(number.longValue())
            .map(ProjectDeployment::getEnvironment);
    }
}
