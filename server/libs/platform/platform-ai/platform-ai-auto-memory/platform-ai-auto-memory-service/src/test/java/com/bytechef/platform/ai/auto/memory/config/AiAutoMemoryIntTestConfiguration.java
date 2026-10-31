/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.ai.auto.memory.config;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryServiceImpl;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * @author Ivica Cardic
 */
@EnableAutoConfiguration(
    excludeName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@Import(LiquibaseConfiguration.class)
@Configuration
public class AiAutoMemoryIntTestConfiguration {

    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-24T10:00:00Z"), ZoneId.of("UTC"));

    @Bean
    AiAutoMemoryService aiAutoMemoryService(AiAutoMemoryRepository aiAutoMemoryRepository) {
        return new AiAutoMemoryServiceImpl(aiAutoMemoryRepository, CLOCK);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AiAutoMemoryIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
