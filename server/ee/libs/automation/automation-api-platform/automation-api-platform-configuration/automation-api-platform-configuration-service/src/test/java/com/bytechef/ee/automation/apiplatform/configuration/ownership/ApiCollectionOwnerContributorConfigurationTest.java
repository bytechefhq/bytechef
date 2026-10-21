/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.ownership;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiCollectionOwnerContributorConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(ApiCollectionOwnerContributorConfiguration.class, ApiCollectionServiceImpl.class);

    @Test
    void testRegistersTheApiCollectionContributorWithoutTheEnterpriseServicesInTheCommunityEdition() {
        applicationContextRunner.run(context -> {
            assertThat(context.getBeansOfType(ProjectDeploymentOwnerContributor.class)
                .values())
                    .extracting(ProjectDeploymentOwnerContributor::getProjectDeploymentOwner)
                    .containsExactly(ProjectDeploymentOwner.API_COLLECTION);
            assertThat(context).doesNotHaveBean(ApiCollectionServiceImpl.class);
        });
    }
}
