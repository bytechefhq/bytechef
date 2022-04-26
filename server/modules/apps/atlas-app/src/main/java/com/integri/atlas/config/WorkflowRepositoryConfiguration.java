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

package com.integri.atlas.config;

import com.integri.atlas.engine.coordinator.workflow.WorkflowRepositoryChain;
import com.integri.atlas.engine.coordinator.workflow.repository.JSONWorkflowMapper;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowMapper;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowMapperChain;
import com.integri.atlas.engine.coordinator.workflow.repository.WorkflowRepository;
import com.integri.atlas.engine.coordinator.workflow.repository.YAMLWorkflowMapper;
import com.integri.atlas.workflow.repository.git.GitWorkflowRepository;
import com.integri.atlas.workflow.repository.resource.ResourceBasedWorkflowRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableConfigurationProperties(AtlasProperties.class)
public class WorkflowRepositoryConfiguration {

    @Bean
    @Primary
    WorkflowRepositoryChain workflowRepository(List<WorkflowRepository> aRepositories) {
        return new WorkflowRepositoryChain(aRepositories);
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "atlas.workflow-repository.classpath.enabled", havingValue = "true")
    ResourceBasedWorkflowRepository resourceBasedWorkflowRepository() {
        return new ResourceBasedWorkflowRepository(workflowMapper());
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(name = "atlas.workflow-repository.filesystem.enabled", havingValue = "true")
    ResourceBasedWorkflowRepository fileSystemBasedWorkflowRepository(
        @Value("${atlas.workflow-repository.filesystem.location-pattern}") String aBasePath
    ) {
        return new ResourceBasedWorkflowRepository(String.format("file:%s", aBasePath), workflowMapper());
    }

    @Bean
    @Order(3)
    @ConditionalOnProperty(name = "atlas.workflow-repository.git.enabled", havingValue = "true")
    GitWorkflowRepository gitWorkflowRepository(AtlasProperties atlasProperties) {
        WorkflowRepositoryProperties.GitProperties gitProperties = atlasProperties.getWorkflowRepository().getGit();

        return new GitWorkflowRepository(
            gitProperties.getUrl(),
            gitProperties.getBranch(),
            gitProperties.getSearchPaths(),
            gitProperties.getUsername(),
            gitProperties.getPassword(),
            workflowMapper()
        );
    }

    @Bean
    @Primary
    WorkflowMapper workflowMapper() {
        return new WorkflowMapperChain(List.of(jsonWorkflowMapper(), yamlWorkflowMapper()));
    }

    @Bean
    JSONWorkflowMapper jsonWorkflowMapper() {
        return new JSONWorkflowMapper();
    }

    @Bean
    YAMLWorkflowMapper yamlWorkflowMapper() {
        return new YAMLWorkflowMapper();
    }
}
