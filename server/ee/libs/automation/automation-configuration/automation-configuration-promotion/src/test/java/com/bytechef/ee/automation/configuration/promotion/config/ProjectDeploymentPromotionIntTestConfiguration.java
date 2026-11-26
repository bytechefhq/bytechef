/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.config;

import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.promotion.test.config.PromotionIntTestConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.automation.configuration", "com.bytechef.commons.util",
        "com.bytechef.ee.automation.configuration.promotion", "com.bytechef.encryption", "com.bytechef.jackson.config",
        "com.bytechef.platform.category", "com.bytechef.platform.configuration.workflow.diff",
        "com.bytechef.platform.connection", "com.bytechef.platform.tag"
    },
    excludeFilters = @Filter(
        type = FilterType.REGEX,
        pattern = "com\\.bytechef\\.automation\\.configuration\\.facade\\.AutomationSearchFacadeImpl"))
@EnableAutoConfiguration
@Import(AutomationMethodSecurityConfiguration.class)
@Configuration
public class ProjectDeploymentPromotionIntTestConfiguration {

    @Bean
    ProjectDeploymentOwnerContributor apiCollectionProjectDeploymentOwnerContributor(
        ApiCollectionClaimedProjectDeploymentIds apiCollectionClaimedProjectDeploymentIds) {

        return ProjectDeploymentOwnerContributor.of(
            ProjectDeploymentOwner.API_COLLECTION, apiCollectionClaimedProjectDeploymentIds::getConditionTemplate);
    }

    @Bean
    WorkspaceDataTableFacade workspaceDataTableFacade() {
        return mock(WorkspaceDataTableFacade.class);
    }

    @Import(PromotionIntTestConfiguration.class)
    static class SharedPromotionIntTestConfiguration {
    }
}
