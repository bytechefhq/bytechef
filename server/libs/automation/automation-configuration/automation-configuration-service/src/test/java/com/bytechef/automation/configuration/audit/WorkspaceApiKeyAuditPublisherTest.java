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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ivica Cardic
 */
class WorkspaceApiKeyAuditPublisherTest {

    private final List<Object> publishedEvents = new ArrayList<>();
    private final ApplicationEventPublisher applicationEventPublisher = event -> publishedEvents.add(event);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void testPublishUsesCurrentUserLoginAsPrincipal() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("admin@localhost.com", null, List.of()));

        WorkspaceApiKeyAuditPublisher workspaceApiKeyAuditPublisher = createPublisher(applicationEventPublisher);

        workspaceApiKeyAuditPublisher.publish(WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_DELETED, 7L, null);

        AuditEvent auditEvent = getSingleAuditEvent();

        assertThat(auditEvent.getPrincipal()).isEqualTo("admin@localhost.com");
        assertThat(auditEvent.getType()).isEqualTo("WORKSPACE_API_KEY_DELETED");
        assertThat(auditEvent.getData()).containsExactly(Map.entry("workspaceApiKeyId", "7"));
    }

    @Test
    void testPublishFallsBackToSystemPrincipalWithoutAuthenticatedUser() {
        WorkspaceApiKeyAuditPublisher workspaceApiKeyAuditPublisher = createPublisher(applicationEventPublisher);

        workspaceApiKeyAuditPublisher.publish(WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_DELETED, 7L, null);

        assertThat(getSingleAuditEvent().getPrincipal()).isEqualTo("SYSTEM");
    }

    @Test
    void testPublishKeepsRealWorkspaceApiKeyIdOverCallerData() {
        WorkspaceApiKeyAuditPublisher workspaceApiKeyAuditPublisher = createPublisher(applicationEventPublisher);

        workspaceApiKeyAuditPublisher.publish(
            WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_CREATED, 7L,
            Map.of("workspaceApiKeyId", "999", "workspaceId", "5"));

        assertThat(getSingleAuditEvent().getData())
            .containsOnly(Map.entry("workspaceApiKeyId", "7"), Map.entry("workspaceId", "5"));
    }

    @Test
    void testPublishSwallowsPublisherFailureAndCountsIt() {
        SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();
        StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory();

        staticListableBeanFactory.addBean("meterRegistry", simpleMeterRegistry);

        WorkspaceApiKeyAuditPublisher workspaceApiKeyAuditPublisher = new WorkspaceApiKeyAuditPublisher(
            event -> {
                throw new IllegalStateException("audit store unavailable");
            }, staticListableBeanFactory.getBeanProvider(MeterRegistry.class));

        assertThatCode(
            () -> workspaceApiKeyAuditPublisher.publish(WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_DELETED, 7L, null))
                .doesNotThrowAnyException();

        Counter counter = simpleMeterRegistry.get("bytechef_workspace_api_key_audit_failed")
            .counter();

        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testPublishDefersUntilTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();

        WorkspaceApiKeyAuditPublisher workspaceApiKeyAuditPublisher = createPublisher(applicationEventPublisher);

        workspaceApiKeyAuditPublisher.publish(WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_DELETED, 7L, null);

        assertThat(publishedEvents).isEmpty();

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        transactionSynchronizations.forEach(TransactionSynchronization::afterCommit);

        assertThat(getSingleAuditEvent().getType()).isEqualTo("WORKSPACE_API_KEY_DELETED");
    }

    @Test
    void testPublishIsDroppedWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        WorkspaceApiKeyAuditPublisher workspaceApiKeyAuditPublisher = createPublisher(applicationEventPublisher);

        workspaceApiKeyAuditPublisher.publish(WorkspaceApiKeyAuditEvent.WORKSPACE_API_KEY_DELETED, 7L, null);

        List<TransactionSynchronization> transactionSynchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        transactionSynchronizations.forEach(
            transactionSynchronization -> transactionSynchronization.afterCompletion(
                TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(publishedEvents).isEmpty();
    }

    private AuditEvent getSingleAuditEvent() {
        assertThat(publishedEvents).hasSize(1);

        AuditApplicationEvent auditApplicationEvent = (AuditApplicationEvent) publishedEvents.getFirst();

        return auditApplicationEvent.getAuditEvent();
    }

    private static WorkspaceApiKeyAuditPublisher createPublisher(ApplicationEventPublisher applicationEventPublisher) {
        StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory();

        return new WorkspaceApiKeyAuditPublisher(
            applicationEventPublisher, staticListableBeanFactory.getBeanProvider(MeterRegistry.class));
    }
}
