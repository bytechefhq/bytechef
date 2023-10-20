
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

package com.bytechef.atlas.service.config;

import com.bytechef.atlas.config.WorkflowConfiguration;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.repository.config.WorkflowMapperConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        "com.bytechef.atlas.repository.jdbc"
    })
@EnableAutoConfiguration
@Import({
    WorkflowConfiguration.class, WorkflowMapperConfiguration.class
})
@SpringBootConfiguration
public class WorkflowIntTestConfiguration {

    @MockBean
    private EventPublisher eventPublisher;

    @MockBean
    private MessageBroker messageBroker;

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @EnableCaching
    @TestConfiguration
    public static class CacheConfiguration {
    }

    @EnableJdbcRepositories(basePackages = "com.bytechef.atlas.repository.jdbc")
    public static class WorkflowIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
