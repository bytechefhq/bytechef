
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.server.config;

import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.repository.resource.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.configuration.repository.resource.FilesystemResourceWorkflowRepository;
import com.bytechef.atlas.configuration.repository.resource.config.ResourceWorkflowRepositoryProperties;
import com.bytechef.helios.configuration.constant.ProjectConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.Map;

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
    @ConditionalOnProperty(prefix = "bytechef", name = "workflow.repository.classpath.enabled", havingValue = "true")
    WorkflowRepository classpathBasedWorkflowRepository(
        @Value("${bytechef.workflow.repository.classpath.projects.location-pattern}") String locationPattern) {

        if (logger.isInfoEnabled()) {
            logger.info(
                "Workflow repository type enabled: classpath, location pattern for projects: {}", locationPattern);
        }

        return new ClassPathResourceWorkflowRepository(
            resourcePatternResolver,
            new ResourceWorkflowRepositoryProperties(
                Map.of(ProjectConstants.PROJECT_WORKFLOW_TYPE, locationPattern), "classpath"));
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(prefix = "bytechef", name = "workflow.repository.filesystem.enabled", havingValue = "true")
    WorkflowRepository filesystemBasedWorkflowRepository(
        @Value("${bytechef.workflow.repository.filesystem.projects.location-pattern}") String locationPattern) {

        if (logger.isInfoEnabled()) {
            logger.info(
                "Workflow repository type enabled: filesystem, location pattern for projects: {}", locationPattern);
        }

        return new FilesystemResourceWorkflowRepository(
            resourcePatternResolver,
            new ResourceWorkflowRepositoryProperties(
                Map.of(ProjectConstants.PROJECT_WORKFLOW_TYPE, locationPattern), "file"));
    }
}
