/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.config;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.ee.embedded.connected.user.service")
@EnableAutoConfiguration
@Import(LiquibaseConfiguration.class)
@Configuration
public class ConnectedUserIntTestConfiguration {

    private static final String MATCH_NOTHING = "%1$s = 0";

    @Bean
    ProjectOwnerContributor embeddedAutomationWorkflowProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_AUTOMATION_WORKFLOW_PROJECT, MATCH_NOTHING);
    }

    @Bean
    ProjectOwnerContributor embeddedConnectedUserProjectOwnerContributor() {
        return ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CONNECTED_USER_PROJECT, MATCH_NOTHING);
    }

    @Bean
    ProjectDeploymentOwnerContributor embeddedProjectDeploymentOwnerContributor() {
        return ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, MATCH_NOTHING);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class ConnectedUserIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
