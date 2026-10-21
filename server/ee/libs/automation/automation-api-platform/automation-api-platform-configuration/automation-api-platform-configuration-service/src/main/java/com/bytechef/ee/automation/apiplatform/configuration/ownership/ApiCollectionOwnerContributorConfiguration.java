/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.ownership;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class ApiCollectionOwnerContributorConfiguration {

    private static final String API_COLLECTION_DEPLOYMENT_CLAIM_CONDITION =
        "EXISTS (SELECT 1 FROM api_collection WHERE api_collection.project_deployment_id = %1$s)";

    @Bean
    ProjectDeploymentOwnerContributor apiCollectionProjectDeploymentOwnerContributor() {
        return ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.API_COLLECTION, API_COLLECTION_DEPLOYMENT_CLAIM_CONDITION);
    }
}
