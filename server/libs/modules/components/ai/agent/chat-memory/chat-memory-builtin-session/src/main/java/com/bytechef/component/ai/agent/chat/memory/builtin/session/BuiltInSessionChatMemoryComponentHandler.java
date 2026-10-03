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

package com.bytechef.component.ai.agent.chat.memory.builtin.session;

import static com.bytechef.component.ai.agent.chat.memory.builtin.session.constant.BuiltInSessionChatMemoryConstants.BUILT_IN_SESSION_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.builtin.session.cluster.BuiltInSessionChatMemory;
import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.BuiltInSessionRepositoryFactory;
import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.BuiltInSessionRepositoryFactory.BuiltInSessionStore;
import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.ExpiredSessionCleaner;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.ai.agent.SlidingExpirySessionRepository;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.SessionRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.format.datetime.standard.DurationFormatterUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(BUILT_IN_SESSION_CHAT_MEMORY + "_v1_ComponentHandler")
public class BuiltInSessionChatMemoryComponentHandler implements ComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(BuiltInSessionChatMemoryComponentHandler.class);

    private static final String SESSION_TIME_TO_LIVE = "bytechef.ai.memory.session-time-to-live";
    private static final String TENANT_MODE = "bytechef.tenant.mode";

    private final Clock clock;
    private final ComponentDefinition componentDefinition;
    private final AutoCloseable closeable;
    private final ExpiredSessionCleaner expiredSessionCleaner;
    private final AtomicBoolean missingTenantServiceReported = new AtomicBoolean();
    private final boolean multiTenantMode;
    private final ObjectProvider<TenantService> tenantServiceProvider;

    @Autowired
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public BuiltInSessionChatMemoryComponentHandler(
        @Autowired(required = false) @Nullable JdbcTemplate jdbcTemplate, Environment environment,
        ObjectProvider<TenantService> tenantServiceProvider) {

        this(jdbcTemplate, environment, tenantServiceProvider, Clock.systemUTC());
    }

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    BuiltInSessionChatMemoryComponentHandler(
        @Nullable JdbcTemplate jdbcTemplate, Environment environment,
        ObjectProvider<TenantService> tenantServiceProvider, Clock clock) {

        Duration sessionTimeToLive = DurationFormatterUtils.detectAndParse(
            environment.getRequiredProperty(SESSION_TIME_TO_LIVE));

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(
            environment, jdbcTemplate);

        SessionRepository sessionRepository = new SlidingExpirySessionRepository(
            builtInSessionStore.sessionRepository(), sessionTimeToLive, clock);

        this.clock = clock;
        this.closeable = builtInSessionStore.closeable();
        this.expiredSessionCleaner = new ExpiredSessionCleaner(sessionRepository, this::getTenantIds);
        this.multiTenantMode = "multi".equalsIgnoreCase(environment.getProperty(TENANT_MODE));
        this.tenantServiceProvider = tenantServiceProvider;

        this.componentDefinition = component(BUILT_IN_SESSION_CHAT_MEMORY)
            .title("Built-in Session Repository")
            .description("Built-in storage backend for Session Chat Memory.")
            .icon("path:assets/built-in-session-chat-memory.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .clusterElements(BuiltInSessionChatMemory.of(sessionRepository));
    }

    @PreDestroy
    public void destroy() {
        try {
            closeable.close();
        } catch (Exception exception) {
            log.warn("Failed to close built-in session repository client", exception);
        }
    }

    @Scheduled(
        fixedDelayString = "${bytechef.ai.memory.session-cleanup-interval}",
        initialDelayString = "${bytechef.ai.memory.session-cleanup-interval}")
    public void deleteExpiredSessions() {
        int deletedCount = expiredSessionCleaner.deleteExpiredSessions(clock.instant());

        log.debug("Deleted {} expired built-in sessions", deletedCount);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private List<String> getTenantIds() {
        TenantService tenantService = tenantServiceProvider.getIfAvailable();

        if (tenantService == null) {
            if (multiTenantMode && missingTenantServiceReported.compareAndSet(false, true)) {
                log.error(
                    "{}=multi but no TenantService is available; expired built-in sessions are deleted only for " +
                        "the default tenant",
                    TENANT_MODE);
            }

            return List.of(TenantContext.DEFAULT_TENANT_ID);
        }

        if (!tenantService.isMultiTenantEnabled()) {
            return List.of(TenantContext.DEFAULT_TENANT_ID);
        }

        return tenantService.getTenantIds();
    }
}
