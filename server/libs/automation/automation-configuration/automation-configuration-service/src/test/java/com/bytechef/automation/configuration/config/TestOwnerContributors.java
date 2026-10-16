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

package com.bytechef.automation.configuration.config;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author Ivica Cardic
 */
@TestConfiguration
public class TestOwnerContributors {

    private final Set<Long> catalogIds = new HashSet<>();
    private final Set<Long> connectedUserIds = new HashSet<>();
    private final Set<Long> apiCollectionIds = new HashSet<>();
    private final Set<Long> a2aServerIds = new HashSet<>();
    private final Set<Long> embeddedIds = new HashSet<>();
    private final Set<Long> mcpServerIds = new HashSet<>();

    public void addCatalogId(long projectId) {
        catalogIds.add(projectId);
    }

    public void addConnectedUserId(long projectId) {
        connectedUserIds.add(projectId);
    }

    public void addDeploymentId(ProjectDeploymentOwner projectDeploymentOwner, long projectDeploymentId) {
        Set<Long> projectDeploymentIds = switch (projectDeploymentOwner) {
            case API_COLLECTION -> apiCollectionIds;
            case A2A_SERVER -> a2aServerIds;
            case EMBEDDED -> embeddedIds;
            case MCP_SERVER -> mcpServerIds;
        };

        projectDeploymentIds.add(projectDeploymentId);
    }

    public void clear() {
        catalogIds.clear();
        connectedUserIds.clear();
        apiCollectionIds.clear();
        a2aServerIds.clear();
        embeddedIds.clear();
        mcpServerIds.clear();
    }

    @Bean
    ProjectOwnerContributor catalogProjectOwnerContributor() {
        return projectContributor(ProjectOwner.EMBEDDED_CATALOG, catalogIds);
    }

    @Bean
    ProjectOwnerContributor connectedUserProjectOwnerContributor() {
        return projectContributor(ProjectOwner.EMBEDDED_CONNECTED_USER, connectedUserIds);
    }

    @Bean
    ProjectDeploymentOwnerContributor apiCollectionProjectDeploymentOwnerContributor() {
        return deploymentContributor(ProjectDeploymentOwner.API_COLLECTION, apiCollectionIds);
    }

    @Bean
    ProjectDeploymentOwnerContributor a2aServerProjectDeploymentOwnerContributor() {
        return deploymentContributor(ProjectDeploymentOwner.A2A_SERVER, a2aServerIds);
    }

    @Bean
    ProjectDeploymentOwnerContributor embeddedProjectDeploymentOwnerContributor() {
        return deploymentContributor(ProjectDeploymentOwner.EMBEDDED, embeddedIds);
    }

    @Bean
    ProjectDeploymentOwnerContributor mcpServerProjectDeploymentOwnerContributor() {
        return deploymentContributor(ProjectDeploymentOwner.MCP_SERVER, mcpServerIds);
    }

    private ProjectOwnerContributor projectContributor(ProjectOwner projectOwner, Set<Long> ids) {
        return new ProjectOwnerContributor() {

            @Override
            public ProjectOwner getProjectOwner() {
                return projectOwner;
            }

            @Override
            public String getProjectClaimCondition(String projectIdColumn) {
                return condition(projectIdColumn, ids);
            }
        };
    }

    private ProjectDeploymentOwnerContributor deploymentContributor(
        ProjectDeploymentOwner projectDeploymentOwner, Set<Long> ids) {

        return new ProjectDeploymentOwnerContributor() {

            @Override
            public ProjectDeploymentOwner getProjectDeploymentOwner() {
                return projectDeploymentOwner;
            }

            @Override
            public String getProjectDeploymentClaimCondition(String projectDeploymentIdColumn) {
                return condition(projectDeploymentIdColumn, ids);
            }
        };
    }

    private static String condition(String column, Set<Long> ids) {
        if (ids.isEmpty()) {
            return "1 = 0";
        }

        return column + " IN (" + ids.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(", ")) + ")";
    }
}
