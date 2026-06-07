/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.audit;

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
 * Publishes audit events for Context Store source lifecycle mutations.
 *
 * <p>
 * Registered as a plain {@code @Component} (not {@code @ConditionalOnEEVersion}) so the facade can inject it directly
 * without conditional-bean wiring problems in integration tests. Failures must NOT propagate to callers: if the
 * security context cannot be resolved, the principal falls back to {@code "SYSTEM"}; any other failure is swallowed so
 * the surrounding business transaction is never broken by best-effort audit emission.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
public class ContextStoreSourceAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(ContextStoreSourceAuditPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public ContextStoreSourceAuditPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Convenience overload for events that carry no additional data beyond the implicit {@code sourceId}.
     */
    public void publish(ContextStoreSourceAuditEvent eventType, long sourceId) {
        publish(eventType, sourceId, Map.of());
    }

    public void publish(ContextStoreSourceAuditEvent eventType, long sourceId, Map<String, Object> additionalData) {
        try {
            String principal = resolvePrincipal(eventType, sourceId);

            Map<String, Object> data = new HashMap<>();

            if (additionalData != null) {
                data.putAll(additionalData);
            }

            data.putIfAbsent("sourceId", String.valueOf(sourceId));

            AuditEvent auditEvent = new AuditEvent(principal, eventType.name(), data);

            applicationEventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
        } catch (RuntimeException exception) {
            // Best-effort: a failure to emit the audit event must never break the surrounding business transaction.
            log.warn(
                "Could not publish audit event {} for Context Store source id={}", eventType, sourceId, exception);
        }
    }

    private String resolvePrincipal(ContextStoreSourceAuditEvent eventType, long sourceId) {
        try {
            return SecurityUtils.fetchCurrentUserLogin()
                .orElse("SYSTEM");
        } catch (RuntimeException exception) {
            log.warn(
                "Could not resolve principal for audit event {} on Context Store source id={}, using SYSTEM",
                eventType, sourceId, exception);

            return "SYSTEM";
        }
    }
}
