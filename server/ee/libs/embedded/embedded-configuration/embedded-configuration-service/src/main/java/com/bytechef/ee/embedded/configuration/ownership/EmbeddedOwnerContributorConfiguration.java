/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.ownership;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class EmbeddedOwnerContributorConfiguration {

    private static final String CATALOG_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM automation_workflow_project WHERE automation_workflow_project.project_id = %s)";
    private static final String CONNECTED_USER_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM connected_user_project WHERE connected_user_project.project_id = %s)";
    private static final String EMBEDDED_DEPLOYMENT_CLAIM_CONDITION =
        "(EXISTS (SELECT 1 FROM project_deployment owned_deployment JOIN connected_user_project ON " +
            "connected_user_project.project_id = owned_deployment.project_id WHERE owned_deployment.id = %1$s) OR " +
            "EXISTS (SELECT 1 FROM connected_user_reference_deployment WHERE " +
            "connected_user_reference_deployment.project_deployment_id = %1$s))";

    @Bean
    ProjectOwnerContributor embeddedCatalogProjectOwnerContributor() {
        return new ProjectOwnerContributor() {

            @Override
            public ProjectOwner getProjectOwner() {
                return ProjectOwner.EMBEDDED_CATALOG;
            }

            @Override
            public String getProjectClaimCondition(String projectIdColumn) {
                return CATALOG_CLAIM_CONDITION.formatted(projectIdColumn);
            }
        };
    }

    @Bean
    ProjectOwnerContributor embeddedConnectedUserProjectOwnerContributor() {
        return new ProjectOwnerContributor() {

            @Override
            public ProjectOwner getProjectOwner() {
                return ProjectOwner.EMBEDDED_CONNECTED_USER;
            }

            @Override
            public String getProjectClaimCondition(String projectIdColumn) {
                return CONNECTED_USER_CLAIM_CONDITION.formatted(projectIdColumn);
            }
        };
    }

    @Bean
    ProjectDeploymentOwnerContributor embeddedProjectDeploymentOwnerContributor() {
        return new ProjectDeploymentOwnerContributor() {

            @Override
            public ProjectDeploymentOwner getProjectDeploymentOwner() {
                return ProjectDeploymentOwner.EMBEDDED;
            }

            @Override
            public String getProjectDeploymentClaimCondition(String projectDeploymentIdColumn) {
                return EMBEDDED_DEPLOYMENT_CLAIM_CONDITION.formatted(projectDeploymentIdColumn);
            }
        };
    }
}
