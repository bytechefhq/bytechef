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

package com.bytechef.atlas.configuration.repository.git.config;

import com.bytechef.atlas.configuration.repository.annotation.ConditionalOnWorkflowRepositoryGit;
import com.bytechef.atlas.configuration.repository.config.contributor.GitWorkflowRepositoryPropertiesContributor;
import com.bytechef.atlas.configuration.repository.git.GitWorkflowRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnWorkflowRepositoryGit
public class GitWorkflowRepositoryConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GitWorkflowRepositoryConfiguration.class);

    public GitWorkflowRepositoryConfiguration() {
        if (logger.isInfoEnabled()) {
            logger.info("Workflow repository type enabled: git");
        }
    }

    @Bean
    @Order(4)
    GitWorkflowRepository gitWorkflowRepository(List<GitWorkflowRepositoryPropertiesContributor> contributors) {
        Map<Integer, GitWorkflowRepositoryProperties> gitWorkflowRepositoryPropertiesMap = new HashMap<>();

        for (GitWorkflowRepositoryPropertiesContributor contributor : contributors) {
            GitWorkflowRepositoryPropertiesContributor.GitWorkflowRepositoryProperties gitWorkflowRepositoryProperties =
                contributor.getGitWorkflowRepositoryProperties();

            gitWorkflowRepositoryPropertiesMap.put(
                contributor.getType(),
                new GitWorkflowRepositoryProperties(
                    gitWorkflowRepositoryProperties.branch(), gitWorkflowRepositoryProperties.password(),
                    gitWorkflowRepositoryProperties.searchPaths(), gitWorkflowRepositoryProperties.url(),
                    gitWorkflowRepositoryProperties.username()));
        }

        return new GitWorkflowRepository(gitWorkflowRepositoryPropertiesMap);
    }
}
