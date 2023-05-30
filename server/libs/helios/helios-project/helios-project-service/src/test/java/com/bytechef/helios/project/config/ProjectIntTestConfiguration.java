
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

package com.bytechef.helios.project.config;

import com.bytechef.atlas.config.WorkflowConfiguration;
import com.bytechef.event.EventPublisher;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionServiceImpl;
import com.bytechef.message.broker.MessageBroker;

import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.atlas.repository.jdbc", "com.bytechef.category", "com.bytechef.helios.project", "com.bytechef.tag"
    })
@EnableAutoConfiguration
@Import({
    WorkflowConfiguration.class
})
@SpringBootConfiguration
public class ProjectIntTestConfiguration {

    @MockBean
    private ConnectionService connectionService;

    @MockBean
    private EventPublisher eventPublisher;

    @MockBean
    private MessageBroker messageBroker;

    @MockBean
    ComponentDefinitionRegistry componentDefinitionRegistry;

    @Bean
    ComponentDefinitionService componentDefinitionService(ComponentDefinitionRegistry componentDefinitionRegistry) {
        return new ComponentDefinitionServiceImpl(componentDefinitionRegistry);
    }

    @EnableCaching
    @TestConfiguration
    public static class CacheConfiguration {
    }

    @EnableJdbcRepositories(
        basePackages = {
            "com.bytechef.atlas.repository.jdbc", "com.bytechef.category.repository",
            "com.bytechef.helios.project.repository", "com.bytechef.tag.repository"
        })
    public static class ProjectIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
