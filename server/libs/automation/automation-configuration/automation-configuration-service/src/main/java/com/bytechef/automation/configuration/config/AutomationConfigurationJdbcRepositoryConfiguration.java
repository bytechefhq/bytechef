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

import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectOwnershipServiceImpl;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @author Ivica Cardic
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.automation.configuration.repository")
class AutomationConfigurationJdbcRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean(ProjectOwnershipService.class)
    ProjectOwnershipService projectOwnershipService(
        JdbcClient jdbcClient,
        ObjectProvider<ProjectDeploymentOwnerContributor> projectDeploymentOwnerContributorProvider,
        ObjectProvider<ProjectOwnerContributor> projectOwnerContributorProvider) {

        return new ProjectOwnershipServiceImpl(
            jdbcClient, projectDeploymentOwnerContributorProvider.orderedStream()
                .toList(),
            projectOwnerContributorProvider.orderedStream()
                .toList());
    }
}
