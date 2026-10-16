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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import({
    PostgreSQLContainerConfiguration.class, WorkspaceApiKeyServiceIntTest.AuditEventCollectorConfiguration.class
})
@ProjectIntTestConfigurationSharedMocks
class WorkspaceApiKeyServiceIntTest {

    @Autowired
    private AuditEventCollector auditEventCollector;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private WorkspaceApiKeyRepository workspaceApiKeyRepository;

    @Autowired
    private WorkspaceApiKeyService workspaceApiKeyService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long apiKeyId;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test"));

        workspaceId = workspace.getId();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Long insertedApiKeyId = Objects.requireNonNull(jdbcTemplate.queryForObject(
            "INSERT INTO api_key (name, secret_key, user_id, environment, created_date, created_by, " +
                "last_modified_date, last_modified_by) VALUES ('test', 'secret', 1, 0, now(), 'test', now(), 'test') " +
                "RETURNING id",
            Long.class));

        apiKeyId = insertedApiKeyId;

        auditEventCollector.clear();
    }

    @AfterEach
    void afterEach() {
        workspaceApiKeyRepository.deleteAll();
        workspaceRepository.deleteAll();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM api_key");
    }

    @Test
    void testCreatePublishesCreatedEventAfterCommit() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyService.create(apiKeyId, workspaceId);

        assertThat(auditEventCollector.getAuditEvents())
            .singleElement()
            .satisfies(auditEvent -> {
                assertThat(auditEvent.getType()).isEqualTo("WORKSPACE_API_KEY_CREATED");
                assertThat(auditEvent.getData()).containsOnly(
                    Map.entry("workspaceApiKeyId", String.valueOf(workspaceApiKey.getId())),
                    Map.entry("workspaceId", String.valueOf(workspaceId)));
            });
    }

    @Test
    void testCreateInRolledBackTransactionPublishesNothing() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            workspaceApiKeyService.create(apiKeyId, workspaceId);

            transactionStatus.setRollbackOnly();
        });

        assertThat(workspaceApiKeyRepository.findAll()).isEmpty();
        assertThat(auditEventCollector.getAuditEvents()).isEmpty();
    }

    @Test
    void testDeletePublishesDeletedEvent() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKeyId, workspaceId));

        workspaceApiKeyService.delete(workspaceApiKey.getId());

        assertThat(workspaceApiKeyRepository.findById(workspaceApiKey.getId())).isEmpty();
        assertThat(auditEventCollector.getAuditEvents())
            .singleElement()
            .satisfies(auditEvent -> {
                assertThat(auditEvent.getType()).isEqualTo("WORKSPACE_API_KEY_DELETED");
                assertThat(auditEvent.getData())
                    .containsOnly(Map.entry("workspaceApiKeyId", String.valueOf(workspaceApiKey.getId())));
            });
    }

    @Test
    void testDeleteOfMissingWorkspaceApiKeyPublishesNothing() {
        workspaceApiKeyService.delete(Long.MAX_VALUE);

        assertThat(auditEventCollector.getAuditEvents()).isEmpty();
    }

    @Test
    void testDeleteWorkspaceApiKeyPublishesDeletedEventForBinding() {
        WorkspaceApiKey workspaceApiKey = workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKeyId, workspaceId));

        workspaceApiKeyService.deleteWorkspaceApiKey(apiKeyId);

        assertThat(workspaceApiKeyRepository.findById(workspaceApiKey.getId())).isEmpty();
        assertThat(auditEventCollector.getAuditEvents())
            .extracting(AuditEvent::getType)
            .containsExactly("WORKSPACE_API_KEY_DELETED");
    }

    @Test
    void testDeleteWorkspaceApiKeyWithoutBindingPublishesNothing() {
        workspaceApiKeyService.deleteWorkspaceApiKey(apiKeyId);

        assertThat(auditEventCollector.getAuditEvents()).isEmpty();
    }

    static class AuditEventCollector {

        private final List<AuditEvent> auditEvents = new CopyOnWriteArrayList<>();

        @EventListener
        void onAuditApplicationEvent(AuditApplicationEvent auditApplicationEvent) {
            auditEvents.add(auditApplicationEvent.getAuditEvent());
        }

        void clear() {
            auditEvents.clear();
        }

        List<AuditEvent> getAuditEvents() {
            return List.copyOf(auditEvents);
        }
    }

    @TestConfiguration
    static class AuditEventCollectorConfiguration {

        @Bean
        AuditEventCollector auditEventCollector() {
            return new AuditEventCollector();
        }
    }
}
