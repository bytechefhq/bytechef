/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
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
 * @version ee
 *
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
