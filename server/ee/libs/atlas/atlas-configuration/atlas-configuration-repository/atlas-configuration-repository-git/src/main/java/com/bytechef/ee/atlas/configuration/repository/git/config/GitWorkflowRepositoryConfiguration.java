/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.configuration.repository.git.config;

import com.bytechef.atlas.configuration.repository.annotation.ConditionalOnWorkflowRepositoryGit;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.edition.annotation.ConditionalOnEEVersion;
import com.bytechef.ee.atlas.configuration.repository.git.GitWorkflowRepository;
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
@ConditionalOnEEVersion
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
    GitWorkflowRepository gitWorkflowRepository(ApplicationProperties applicationProperties) {
        return new GitWorkflowRepository(applicationProperties);
    }
}
