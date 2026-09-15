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

package com.bytechef.automation.workflow.execution.security;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reports the environment of an automation job as the environment of the project deployment that ran it: job &rarr;
 * principal job &rarr; {@link ProjectDeployment}. A job with no deployment principal has no environment.
 *
 * @author Ivica Cardic
 */
@Component
public class JobEnvironmentResolver implements ResourceEnvironmentResolver {

    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public JobEnvironmentResolver(
        PrincipalJobService principalJobService, ProjectDeploymentService projectDeploymentService) {

        this.principalJobService = principalJobService;
        this.projectDeploymentService = projectDeploymentService;
    }

    @Override
    public String resourceType() {
        return "Job";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return principalJobService.fetchJobPrincipalId(number.longValue(), PlatformType.AUTOMATION)
            .flatMap(projectDeploymentService::fetchProjectDeployment)
            .map(ProjectDeployment::getEnvironment);
    }
}
