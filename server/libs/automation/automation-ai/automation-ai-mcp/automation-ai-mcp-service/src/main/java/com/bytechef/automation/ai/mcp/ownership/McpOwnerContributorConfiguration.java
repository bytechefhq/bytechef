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

package com.bytechef.automation.ai.mcp.ownership;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class McpOwnerContributorConfiguration {

    private static final String MCP_SERVER_DEPLOYMENT_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM mcp_project WHERE mcp_project.project_deployment_id = %1$s)";

    @Bean
    ProjectDeploymentOwnerContributor mcpServerProjectDeploymentOwnerContributor() {
        return ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.MCP_SERVER, MCP_SERVER_DEPLOYMENT_CLAIM_CONDITION);
    }
}
