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

package com.bytechef.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import({
    PostgreSQLContainerConfiguration.class, WorkspaceApiKeyAuditPublisherIntTest.AuditRecorderConfiguration.class
})
@ProjectIntTestConfigurationSharedMocks
class WorkspaceApiKeyAuditPublisherIntTest {

    private static final String AUDIT_FAILED_COUNTER = "bytechef_workspace_api_key_audit_failed";
    private static final long WORKSPACE_API_KEY_ID = 7L;

    @Autowired
    private AuditEventRecorder auditEventRecorder;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private WorkspaceApiKeyAuditPublisher workspaceApiKeyAuditPublisher;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @BeforeEach
    void beforeEach() {
        auditEventRecorder.reset();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        auditEventRecorder.reset();
    }

    @Test
    void testPublishUsesCurrentUserLoginAsPrincipal() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("admin@localhost.com", null, List.of()));

        workspaceApiKeyAuditPublisher.publish(
            WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_DELETED, WORKSPACE_API_KEY_ID, null);

        assertThat(auditEventRecorder.getAuditEvents())
            .singleElement()
            .satisfies(auditEvent -> {
                assertThat(auditEvent.getPrincipal()).isEqualTo("admin@localhost.com");
                assertThat(auditEvent.getType()).isEqualTo("WORKSPACE_API_KEY_DELETED");
                assertThat(auditEvent.getData()).containsExactly(Map.entry("workspaceApiKeyId", "7"));
            });
    }

    @Test
    void testPublishFallsBackToSystemPrincipalWithoutAuthenticatedUser() {
        workspaceApiKeyAuditPublisher.publish(
            WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_DELETED, WORKSPACE_API_KEY_ID, null);

        assertThat(auditEventRecorder.getAuditEvents())
            .singleElement()
            .extracting(AuditEvent::getPrincipal)
            .isEqualTo("SYSTEM");
    }

    @Test
    void testPublishKeepsRealWorkspaceApiKeyIdOverCallerData() {
        workspaceApiKeyAuditPublisher.publish(
            WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_CREATED, WORKSPACE_API_KEY_ID,
            Map.of("workspaceApiKeyId", "999", "workspaceId", "5"));

        assertThat(auditEventRecorder.getAuditEvents())
            .singleElement()
            .satisfies(auditEvent -> assertThat(auditEvent.getData())
                .containsOnly(Map.entry("workspaceApiKeyId", "7"), Map.entry("workspaceId", "5")));
    }

    @Test
    void testPublishSwallowsPublisherFailureAndCountsIt() {
        Counter counter = meterRegistry.get(AUDIT_FAILED_COUNTER)
            .counter();

        double failuresBefore = counter.count();

        auditEventRecorder.failWith(new IllegalStateException("audit store unavailable"));

        assertThatCode(
            () -> workspaceApiKeyAuditPublisher.publish(
                WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_DELETED, WORKSPACE_API_KEY_ID, null))
                    .doesNotThrowAnyException();

        assertThat(counter.count()).isEqualTo(failuresBefore + 1);
        assertThat(auditEventRecorder.getAuditEvents()).isEmpty();
    }

    @Test
    void testPublishDefersUntilTheSurroundingTransactionCommits() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        Workspace workspace = transactionTemplate.execute(transactionStatus -> {
            Workspace savedWorkspace = workspaceRepository.save(new Workspace("audit-commit"));

            workspaceApiKeyAuditPublisher.publish(
                WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_DELETED, WORKSPACE_API_KEY_ID, null);

            assertThat(auditEventRecorder.getAuditEvents()).isEmpty();

            return savedWorkspace;
        });

        assertThat(workspace).isNotNull();

        try {
            assertThat(workspaceRepository.findById(workspace.getId())).isPresent();
            assertThat(auditEventRecorder.getAuditEvents())
                .singleElement()
                .extracting(AuditEvent::getType)
                .isEqualTo("WORKSPACE_API_KEY_DELETED");
        } finally {
            workspaceRepository.deleteById(workspace.getId());
        }
    }

    @Test
    void testPublishIsDroppedWhenTheSurroundingTransactionRollsBack() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        Workspace workspace = transactionTemplate.execute(transactionStatus -> {
            Workspace savedWorkspace = workspaceRepository.save(new Workspace("audit-rollback"));

            workspaceApiKeyAuditPublisher.publish(
                WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_DELETED, WORKSPACE_API_KEY_ID, null);

            transactionStatus.setRollbackOnly();

            return savedWorkspace;
        });

        assertThat(workspace).isNotNull();
        assertThat(workspaceRepository.findById(workspace.getId())).isEmpty();
        assertThat(auditEventRecorder.getAuditEvents()).isEmpty();
    }

    static class AuditEventRecorder {

        private final List<AuditEvent> auditEvents = new CopyOnWriteArrayList<>();
        private volatile RuntimeException failure;

        @EventListener
        void onAuditApplicationEvent(AuditApplicationEvent auditApplicationEvent) {
            RuntimeException currentFailure = failure;

            if (currentFailure != null) {
                throw currentFailure;
            }

            auditEvents.add(auditApplicationEvent.getAuditEvent());
        }

        void failWith(RuntimeException runtimeException) {
            failure = runtimeException;
        }

        List<AuditEvent> getAuditEvents() {
            return List.copyOf(auditEvents);
        }

        void reset() {
            auditEvents.clear();

            failure = null;
        }
    }

    static class AuditRecorderConfiguration {

        @Bean
        AuditEventRecorder auditEventRecorder() {
            return new AuditEventRecorder();
        }

        @Bean
        SimpleMeterRegistry simpleMeterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
