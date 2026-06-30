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

package com.bytechef.ai.chat.memory.jdbc.config;

import com.bytechef.component.ai.agent.chat.memory.jdbc.util.JdbcChatMemoryUtils;
import com.bytechef.component.ai.agent.chat.memory.jdbc.util.OrderedJdbcChatMemoryRepository;
import javax.sql.DataSource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@Configuration
class JdbcChatMemoryConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "bytechef.ai.memory", name = "provider", havingValue = "jdbc")
    ChatMemoryRepository orderedJdbcChatMemoryRepository(
        JdbcChatMemoryRepository jdbcChatMemoryRepository, JdbcTemplate jdbcTemplate) {

        DataSource dataSource = jdbcTemplate.getDataSource();

        if (dataSource == null) {
            return jdbcChatMemoryRepository;
        }

        JdbcChatMemoryRepositoryDialect dialect = JdbcChatMemoryRepositoryDialect.from(dataSource);

        return new OrderedJdbcChatMemoryRepository(
            jdbcChatMemoryRepository, jdbcTemplate,
            JdbcChatMemoryUtils.getSelectConversationIdsOrderedSql(dialect));
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.memory", name = "provider", havingValue = "jdbc")
    ChatMemory jdbcChatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(500)
            .build();
    }
}
