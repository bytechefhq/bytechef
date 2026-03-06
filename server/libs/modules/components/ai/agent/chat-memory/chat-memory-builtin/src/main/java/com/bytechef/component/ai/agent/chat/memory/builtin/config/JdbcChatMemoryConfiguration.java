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

package com.bytechef.component.ai.agent.chat.memory.builtin.config;

import com.bytechef.config.ApplicationProperties;
import javax.sql.DataSource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(
    prefix = "bytechef.ai.agent.memory", name = "provider", havingValue = "jdbc", matchIfMissing = true)
class JdbcChatMemoryConfiguration {

    private final ApplicationProperties applicationProperties;
    private final DataSource dataSource;

    JdbcChatMemoryConfiguration(ApplicationProperties applicationProperties, DataSource dataSource) {
        this.applicationProperties = applicationProperties;
        this.dataSource = dataSource;
    }

    @Bean
    ChatMemoryRepository chatMemoryRepository() {
        JdbcChatMemoryRepositoryDialect dialect = JdbcChatMemoryRepositoryDialect.from(dataSource);

        if (applicationProperties.getAi()
            .getAgent()
            .getMemory()
            .getJdbc()
            .isInitializeSchema()) {
            initializeSchema(dialect);
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return JdbcChatMemoryRepository.builder()
            .jdbcTemplate(jdbcTemplate)
            .dialect(dialect)
            .build();
    }

    private void initializeSchema(JdbcChatMemoryRepositoryDialect dialect) {
        String schemaScript = getSchemaScript(dialect);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

        populator.addScript(new ClassPathResource(schemaScript));
        populator.setContinueOnError(true);
        populator.execute(dataSource);
    }

    private String getSchemaScript(JdbcChatMemoryRepositoryDialect dialect) {
        String dialectName = dialect.getClass()
            .getSimpleName()
            .toLowerCase();

        if (dialectName.contains("postgres")) {
            return "org/springframework/ai/chat/memory/repository/jdbc/schema-postgresql.sql";
        } else if (dialectName.contains("mysql") || dialectName.contains("maria")) {
            return "org/springframework/ai/chat/memory/repository/jdbc/schema-mysql.sql";
        } else if (dialectName.contains("oracle")) {
            return "org/springframework/ai/chat/memory/repository/jdbc/schema-oracle.sql";
        } else if (dialectName.contains("sqlserver")) {
            return "org/springframework/ai/chat/memory/repository/jdbc/schema-sqlserver.sql";
        } else if (dialectName.contains("hsql")) {
            return "org/springframework/ai/chat/memory/repository/jdbc/schema-hsqldb.sql";
        }

        return "org/springframework/ai/chat/memory/repository/jdbc/schema-postgresql.sql";
    }
}
