
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

package com.bytechef.atlas.job.repository.jdbc.config;

import com.bytechef.atlas.repository.config.WorkflowMapperConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
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
    WorkflowMapperConfiguration.class
})
@SpringBootConfiguration
public class WorkflowRepositoryIntTestConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @EnableCaching
    @TestConfiguration
    public static class CacheConfiguration {
    }

    @EnableJdbcAuditing
    @EnableJdbcRepositories(basePackages = "com.bytechef.atlas.repository.jdbc")
    @TestConfiguration
    public static class JdbcRepositoriesConfiguration {

        @Bean
        AuditorAware<String> auditorProvider() {
            return () -> Optional.of("system");
        }

        @Bean
        public DateTimeProvider auditingDateTimeProvider() {
            return CurrentDateTimeProvider.INSTANCE;
        }
    }
}
