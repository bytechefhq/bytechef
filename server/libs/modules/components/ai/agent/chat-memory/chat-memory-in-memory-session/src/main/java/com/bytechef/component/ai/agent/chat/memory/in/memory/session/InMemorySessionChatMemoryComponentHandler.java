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

package com.bytechef.component.ai.agent.chat.memory.in.memory.session;

import static com.bytechef.component.ai.agent.chat.memory.in.memory.session.constant.InMemorySessionChatMemoryConstants.IN_MEMORY_SESSION_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.in.memory.session.cluster.InMemorySessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.ai.agent.SlidingExpirySessionRepository;
import com.bytechef.platform.component.definition.ai.agent.TenantRoutingSessionRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.format.datetime.standard.DurationFormatterUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(IN_MEMORY_SESSION_CHAT_MEMORY + "_v1_ComponentHandler")
public class InMemorySessionChatMemoryComponentHandler implements ComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(InMemorySessionChatMemoryComponentHandler.class);

    private static final String SESSION_TIME_TO_LIVE = "bytechef.ai.memory.session-time-to-live";

    private final Clock clock;
    private final ComponentDefinition componentDefinition;
    private final TenantRoutingSessionRepository sessionRepository = new TenantRoutingSessionRepository(
        tenantId -> InMemorySessionRepository.builder()
            .build());

    @Autowired
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public InMemorySessionChatMemoryComponentHandler(Environment environment) {
        this(environment, Clock.systemUTC());
    }

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    InMemorySessionChatMemoryComponentHandler(Environment environment, Clock clock) {
        Duration sessionTimeToLive = DurationFormatterUtils.detectAndParse(
            environment.getRequiredProperty(SESSION_TIME_TO_LIVE));

        this.clock = clock;
        this.componentDefinition = component(IN_MEMORY_SESSION_CHAT_MEMORY)
            .title("In-memory Session Repository")
            .description("In-memory storage backend for Session Chat Memory.")
            .icon("path:assets/in-memory-session-chat-memory.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .clusterElements(
                InMemorySessionChatMemory.of(
                    new SlidingExpirySessionRepository(sessionRepository, sessionTimeToLive, clock)));
    }

    @Scheduled(
        fixedDelayString = "${bytechef.ai.memory.session-cleanup-interval}",
        initialDelayString = "${bytechef.ai.memory.session-cleanup-interval}")
    public void deleteExpiredSessions() {
        Map<String, Integer> deletedCounts = sessionRepository.deleteExpiredSessionsOfEveryTenant(clock.instant());

        for (Map.Entry<String, Integer> entry : deletedCounts.entrySet()) {
            int deletedCount = entry.getValue();

            if (deletedCount > 0) {
                log.info("Deleted {} expired in-memory sessions for tenant {}", deletedCount, entry.getKey());
            }
        }
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
