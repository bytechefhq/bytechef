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

package com.bytechef.automation.ai.a2a.ownership;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class A2aOwnerContributorConfiguration {

    private static final String A2A_SERVER_DEPLOYMENT_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM a2a_project WHERE a2a_project.project_deployment_id = %1$s)";
    private static final String A2A_SERVER_NAME =
        "(SELECT owner_a2a_server.name FROM a2a_project owner_a2a_project JOIN a2a_server owner_a2a_server ON " +
            "owner_a2a_server.id = owner_a2a_project.a2a_server_id WHERE owner_a2a_project.project_deployment_id = %1$s "
            +
            "ORDER BY owner_a2a_server.id LIMIT 1)";

    @Bean
    ProjectDeploymentOwnerContributor a2aServerProjectDeploymentOwnerContributor() {
        return ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.A2A_SERVER, A2A_SERVER_DEPLOYMENT_CLAIM_CONDITION, A2A_SERVER_NAME);
    }
}
