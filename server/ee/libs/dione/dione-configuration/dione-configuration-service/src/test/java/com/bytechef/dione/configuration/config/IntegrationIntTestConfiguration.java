
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

package com.bytechef.dione.configuration.config;

import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.configuration.service.WorkflowServiceImpl;

import java.util.List;

import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.atlas.configuration.repository.jdbc", "com.bytechef.category", "com.bytechef.dione.configuration",
        "com.bytechef.liquibase.config", "com.bytechef.tag"
    })
@EnableAutoConfiguration
@Configuration
public class IntegrationIntTestConfiguration {

    @Bean
    public WorkflowService workflowService(
        CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
        List<WorkflowRepository> workflowRepositories) {

        return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
    }

    @EnableCaching
    @TestConfiguration
    public static class CacheConfiguration {
    }

    @EnableJdbcRepositories(
        basePackages = {
            "com.bytechef.atlas.configuration.repository.jdbc", "com.bytechef.category.repository",
            "com.bytechef.dione.configuration.repository", "com.bytechef.tag.repository"
        })
    public static class IntegrationIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
