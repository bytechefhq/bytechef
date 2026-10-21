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

package com.bytechef.automation.configuration.teardown;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Disables every enabled project deployment of the tenant, which unregisters provider webhooks and cancels trigger
 * schedules.
 *
 * @author Ivica Cardic
 */
@Component
@Order(100)
class ProjectDeploymentTenantTeardownContributor implements TenantTeardownContributor {

    private static final Logger log = LoggerFactory.getLogger(ProjectDeploymentTenantTeardownContributor.class);

    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    ProjectDeploymentTenantTeardownContributor(
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService) {

        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
    }

    @Override
    public List<String> onDeletionRequested(String tenantId) {
        List<String> failures = new ArrayList<>();

        for (ProjectDeployment projectDeployment : projectDeploymentService.getProjectDeployments()) {
            if (!projectDeployment.isEnabled()) {
                continue;
            }

            long projectDeploymentId = Objects.requireNonNull(projectDeployment.getId());

            try {
                projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, false);
            } catch (RuntimeException e) {
                log.error(
                    "Unable to disable project deployment {} of tenant {}", projectDeploymentId, tenantId, e);

                failures.add("projectDeployment " + projectDeploymentId + ": " + e.getMessage());
            }
        }

        return failures;
    }
}
