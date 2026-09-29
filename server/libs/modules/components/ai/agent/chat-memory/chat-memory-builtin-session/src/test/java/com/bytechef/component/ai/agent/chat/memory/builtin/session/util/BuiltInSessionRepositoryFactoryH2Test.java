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

package com.bytechef.component.ai.agent.chat.memory.builtin.session.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
class BuiltInSessionRepositoryFactoryH2Test {

    private static final String CHANGELOG =
        "classpath:config/liquibase/changelog/platform/ai/chat_memory/20261002000001_spring_ai_session_schema.xml";

    @Test
    void testJdbcProviderStoresSessionsInTheTablesTheChangelogCreatesOnH2() throws LiquibaseException {
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:" + System.nanoTime() +
                ";DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=KEY,VALUE,USER",
            "sa", "");

        SpringLiquibase springLiquibase = new SpringLiquibase();

        springLiquibase.setChangeLog(CHANGELOG);
        springLiquibase.setDataSource(driverManagerDataSource);
        springLiquibase.setResourceLoader(new DefaultResourceLoader());

        springLiquibase.afterPropertiesSet();

        BuiltInSessionRepositoryFactory.BuiltInSessionStore builtInSessionStore =
            BuiltInSessionRepositoryFactory.create(
                new MockEnvironment().withProperty("bytechef.ai.memory.provider", "jdbc"),
                new JdbcTemplate(driverManagerDataSource));

        SessionRepository sessionRepository = builtInSessionStore.sessionRepository();

        sessionRepository.save(Session.builder()
            .id("conversation-1")
            .userId("bytechef")
            .createdAt(Instant.now())
            .build());

        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId("conversation-1")
            .message(new UserMessage("hello"))
            .build());

        List<SessionEvent> sessionEvents = sessionRepository.findEvents("conversation-1", EventFilter.all());

        assertThat(sessionEvents)
            .extracting(SessionEvent::getMessage)
            .extracting(Message::getText)
            .containsExactly("hello");
    }
}
