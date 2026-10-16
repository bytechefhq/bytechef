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

import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes audit events for workspace API key binding mutations.
 *
 * <p>
 * Inside an active transaction the event is published only after it commits, so a rolled-back mutation is never
 * audited. Failures never propagate to callers: they are logged and counted in
 * {@code bytechef_workspace_api_key_audit_failed}. Unauthenticated callers get the {@code "SYSTEM"} principal.
 *
 * @author Ivica Cardic
 */
@Component
public class WorkspaceApiKeyAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceApiKeyAuditPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final @Nullable Counter auditFailureCounter;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public WorkspaceApiKeyAuditPublisher(
        ApplicationEventPublisher applicationEventPublisher, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.applicationEventPublisher = applicationEventPublisher;

        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        this.auditFailureCounter = meterRegistry == null ? null
            : Counter.builder("bytechef_workspace_api_key_audit_failed")
                .description(
                    "Workspace API key audit events that failed to publish. Non-zero values indicate a gap in the " +
                        "audit trail.")
                .register(meterRegistry);
    }

    public void publish(
        WorkspaceApiKeyAuditEvent eventType, long workspaceApiKeyId, @Nullable Map<String, Object> additionalData) {

        String principal = resolvePrincipal(eventType, workspaceApiKeyId);

        Map<String, Object> data = new HashMap<>();

        if (additionalData != null) {
            data.putAll(additionalData);
        }

        data.put("workspaceApiKeyId", String.valueOf(workspaceApiKeyId));

        AuditEvent auditEvent = new AuditEvent(principal, eventType.name(), data);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    publishAuditEvent(auditEvent);
                }
            });
        } else {
            publishAuditEvent(auditEvent);
        }
    }

    private void publishAuditEvent(AuditEvent auditEvent) {
        try {
            applicationEventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
        } catch (Exception exception) {
            if (auditFailureCounter != null) {
                auditFailureCounter.increment();
            }

            log.error(
                "Failed to publish workspace API key audit event {} (data={})", auditEvent.getType(),
                auditEvent.getData(), exception);
        }
    }

    private static String resolvePrincipal(WorkspaceApiKeyAuditEvent eventType, long workspaceApiKeyId) {
        try {
            return SecurityUtils.fetchCurrentUserLogin()
                .orElse("SYSTEM");
        } catch (RuntimeException exception) {
            log.warn(
                "Could not resolve principal for audit event {} on workspace API key id={}, using SYSTEM",
                eventType, workspaceApiKeyId, exception);

            return "SYSTEM";
        }
    }
}
