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

package com.bytechef.atlas.configuration.repository.resource.config;

import com.bytechef.atlas.configuration.repository.annotation.ConditionalOnWorkflowRepositoryClasspath;
import com.bytechef.atlas.configuration.repository.annotation.ConditionalOnWorkflowRepositoryFilesystem;
import com.bytechef.atlas.configuration.repository.resource.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.configuration.repository.resource.FilesystemResourceWorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ResourceWorkflowRepositoryConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ResourceWorkflowRepositoryConfiguration.class);

    private final ResourcePatternResolver resourcePatternResolver;

    public ResourceWorkflowRepositoryConfiguration(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @Bean
    @Order(1)
    @ConditionalOnWorkflowRepositoryClasspath
    ClassPathResourceWorkflowRepository classpathBasedWorkflowRepository(
        @Value("${bytechef.workflow.repository.classpath.location-pattern:workflows/**/*.{json,yml,yaml}}") String locationPattern) {

        if (logger.isInfoEnabled()) {
            logger.info("Workflow repository type enabled: classpath");
        }

        return new ClassPathResourceWorkflowRepository(locationPattern, resourcePatternResolver);
    }

    @Bean
    @Order(2)
    @ConditionalOnWorkflowRepositoryFilesystem
    FilesystemResourceWorkflowRepository filesystemResourceWorkflowRepository(
        @Value("${bytechef.workflow.repository.filesystem.location-pattern:${user.home}/bytechef/data/workflows/**/*.{json,yml,yaml}}") String locationPattern) {

        if (logger.isInfoEnabled()) {
            logger.info("Workflow repository type enabled: filesystem");
        }

        return new FilesystemResourceWorkflowRepository(locationPattern, resourcePatternResolver);
    }
}
