/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.config;

import com.integri.atlas.repository.git.workflow.GitWorkflowRepository;
import com.integri.atlas.engine.config.PiperProperties;
import com.integri.atlas.engine.coordinator.workflow.WorkflowRepository;
import com.integri.atlas.engine.coordinator.workflow.WorkflowRepositoryChain;
import com.integri.atlas.repository.yaml.workflow.ResourceBasedWorkflowRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration
@EnableConfigurationProperties(PiperProperties.class)
public class WorkflowRepositoryConfiguration {

    @Bean
    @Primary
    WorkflowRepositoryChain workflowRepository(List<WorkflowRepository> aRepositories) {
        return new WorkflowRepositoryChain(aRepositories);
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "piper.workflow-repository.classpath.enabled", havingValue = "true")
    ResourceBasedWorkflowRepository resourceBasedWorkflowRepository() {
        return new ResourceBasedWorkflowRepository();
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(name = "piper.workflow-repository.filesystem.enabled", havingValue = "true")
    ResourceBasedWorkflowRepository fileSystemBasedWorkflowRepository(
        @Value("${piper.workflow-repository.filesystem.location-pattern}") String aBasePath
    ) {
        return new ResourceBasedWorkflowRepository(String.format("file:%s", aBasePath));
    }

    @Bean
    @Order(3)
    @ConditionalOnProperty(name = "piper.workflow-repository.git.enabled", havingValue = "true")
    GitWorkflowRepository gitWorkflowRepository(PiperProperties aProperties) {
        return new GitWorkflowRepository(aProperties.getWorkflowRepository().getGit());
    }
}
