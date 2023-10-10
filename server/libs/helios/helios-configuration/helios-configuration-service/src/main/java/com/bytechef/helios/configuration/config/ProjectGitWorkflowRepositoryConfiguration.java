
/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.helios.configuration.config;

import com.bytechef.helios.configuration.constant.ProjectConstants;
import com.bytechef.atlas.configuration.repository.config.contributor.GitWorkflowRepositoryPropertiesContributor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(ProjectGitWorkflowRepositoryTypeProperties.class)
public class ProjectGitWorkflowRepositoryConfiguration {

    @Bean
    GitWorkflowRepositoryPropertiesContributor projectGitWorkflowRepositoryPropertiesAccessor(
        ProjectGitWorkflowRepositoryTypeProperties projectGitWorkflowRepositoryTypeProperties) {

        return new GitWorkflowRepositoryPropertiesContributor() {

            @Override
            public GitWorkflowRepositoryProperties getGitWorkflowRepositoryProperties() {
                return projectGitWorkflowRepositoryTypeProperties.getProjects();
            }

            @Override
            public int getType() {
                return ProjectConstants.PROJECT_TYPE;
            }
        };
    }
}
