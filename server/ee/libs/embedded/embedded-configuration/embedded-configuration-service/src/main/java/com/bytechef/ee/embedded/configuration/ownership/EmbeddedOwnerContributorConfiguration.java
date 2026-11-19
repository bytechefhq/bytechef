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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class EmbeddedOwnerContributorConfiguration {

    private static final String AUTOMATION_PROJECT_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM automation_project WHERE automation_project.project_id = %1$s)";
    private static final String CONNECTED_USER_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM connected_user_project WHERE connected_user_project.project_id = %1$s)";
    private static final String CONNECTED_USER_DEPLOYMENT_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM project_deployment owned_deployment JOIN connected_user_project ON " +
            "connected_user_project.project_id = owned_deployment.project_id WHERE owned_deployment.id = %1$s)";
    private static final String REFERENCE_DEPLOYMENT_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM connected_user_reference_deployment WHERE " +
            "connected_user_reference_deployment.project_deployment_id = %1$s)";
    private static final String TRIGGER_SUBSCRIPTION_PROJECT_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM connected_user_trigger_subscription WHERE " +
            "connected_user_trigger_subscription.project_id = %1$s)";
    private static final String TRIGGER_SUBSCRIPTION_DEPLOYMENT_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM connected_user_trigger_subscription WHERE " +
            "connected_user_trigger_subscription.project_deployment_id = %1$s)";

    @Bean
    ProjectOwnerContributor embeddedAutomationProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_AUTOMATION_PROJECT,
            AUTOMATION_PROJECT_CLAIM_CONDITION);
    }

    @Bean
    ProjectOwnerContributor embeddedConnectedUserProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CONNECTED_USER_PROJECT, CONNECTED_USER_CLAIM_CONDITION);
    }

    @Bean
    ProjectDeploymentOwnerContributor embeddedConnectedUserProjectDeploymentOwnerContributor() {
        return ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.EMBEDDED, CONNECTED_USER_DEPLOYMENT_CLAIM_CONDITION);
    }

    @Bean
    ProjectDeploymentOwnerContributor embeddedReferenceProjectDeploymentOwnerContributor() {
        return ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.EMBEDDED, REFERENCE_DEPLOYMENT_CLAIM_CONDITION);
    }

    @Bean
    ProjectOwnerContributor embeddedTriggerSubscriptionProjectOwnerContributor() {
        return ProjectOwnerContributor.of(
            ProjectOwner.EMBEDDED_TRIGGER_SUBSCRIPTION, TRIGGER_SUBSCRIPTION_PROJECT_CLAIM_CONDITION);
    }

    @Bean
    ProjectDeploymentOwnerContributor embeddedTriggerSubscriptionProjectDeploymentOwnerContributor() {
        return ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.EMBEDDED, TRIGGER_SUBSCRIPTION_DEPLOYMENT_CLAIM_CONDITION);
    }
}
