/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.ownership;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedOwnerContributorConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(EmbeddedOwnerContributorConfiguration.class);

    @Test
    void testRegistersTheEmbeddedContributorsInTheCommunityEdition() {
        applicationContextRunner.run(EmbeddedOwnerContributorConfigurationTest::assertEmbeddedContributors);
    }

    @Test
    void testRegistersTheEmbeddedContributorsInTheEnterpriseEdition() {
        applicationContextRunner
            .withPropertyValues("bytechef.edition=ee")
            .run(EmbeddedOwnerContributorConfigurationTest::assertEmbeddedContributors);
    }

    private static void assertEmbeddedContributors(AssertableApplicationContext context) {
        assertThat(context.getBeansOfType(ProjectOwnerContributor.class)
            .values())
                .extracting(ProjectOwnerContributor::getProjectOwner)
                .containsExactlyInAnyOrder(ProjectOwner.EMBEDDED_CATALOG, ProjectOwner.EMBEDDED_CONNECTED_USER);
        assertThat(context.getBeansOfType(ProjectDeploymentOwnerContributor.class)
            .values())
                .extracting(ProjectDeploymentOwnerContributor::getProjectDeploymentOwner)
                .containsExactly(ProjectDeploymentOwner.EMBEDDED, ProjectDeploymentOwner.EMBEDDED);
    }
}
