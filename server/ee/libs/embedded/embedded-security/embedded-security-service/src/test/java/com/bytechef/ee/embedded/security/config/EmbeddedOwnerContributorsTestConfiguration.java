/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.config;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@TestConfiguration
public class EmbeddedOwnerContributorsTestConfiguration {

    private static final String MATCH_NOTHING = "%1$s = 0";

    @Bean
    ProjectOwnerContributor embeddedCatalogProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CATALOG, MATCH_NOTHING);
    }

    @Bean
    ProjectOwnerContributor embeddedConnectedUserProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CONNECTED_USER, MATCH_NOTHING);
    }

    @Bean
    ProjectDeploymentOwnerContributor embeddedProjectDeploymentOwnerContributor() {
        return ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, MATCH_NOTHING);
    }
}
