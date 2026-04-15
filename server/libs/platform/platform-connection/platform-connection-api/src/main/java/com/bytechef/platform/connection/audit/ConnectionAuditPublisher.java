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

package com.bytechef.platform.connection.audit;

import com.bytechef.platform.security.util.SecurityUtils;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes audit events for connection-lifecycle mutations.
 *
 * <p>
 * Failures must NOT propagate to callers: the aspect path catches everything, and callers that emit imperatively (e.g.
 * {@link com.bytechef.automation.configuration.facade.ConnectionReassignmentFacade}) wrap this in a try/catch plus
 * {@code bytechef_connection_audit_failed} counter. If the security context cannot be resolved, the principal falls
 * back to {@code "SYSTEM"} rather than failing the surrounding business transaction.
 *
 * <p>
 * Prefer the typed {@link #publish(ConnectionAuditEvent, ConnectionAuditPayload)} overload so call sites declare their
 * payload shape statically. The {@code Map<String, Object>} overload remains for the annotation-driven aspect path,
 * which accumulates keyed data reflectively and cannot use a fixed record shape.
 *
 * @author Ivica Cardic
 */
@Component
public class ConnectionAuditPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAuditPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public ConnectionAuditPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Preferred overload: publish an audit event with a typed payload. Call sites declare the additional-data shape at
     * compile time and the publisher cannot silently drop a misspelled key.
     */
    public void publish(ConnectionAuditEvent eventType, ConnectionAuditPayload payload) {
        publish(eventType, payload.connectionId(), payload.toMap());
    }

    public void publish(ConnectionAuditEvent eventType, long connectionId, Map<String, Object> additionalData) {
        String principal;

        try {
            principal = SecurityUtils.fetchCurrentUserLogin()
                .orElse("SYSTEM");
        } catch (RuntimeException exception) {
            // Pass `exception` as the final SLF4J vararg so the stack trace is preserved —
            // SecurityContext leaks here are subtle and require the trace to diagnose.
            logger.warn(
                "Could not resolve principal for audit event {} on connection id={}, using SYSTEM",
                eventType, connectionId, exception);

            principal = "SYSTEM";
        }

        Map<String, Object> data = new HashMap<>();

        if (additionalData != null) {
            data.putAll(additionalData);
        }

        data.putIfAbsent("connectionId", String.valueOf(connectionId));

        AuditEvent auditEvent = new AuditEvent(principal, eventType.name(), data);

        applicationEventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
    }
}
