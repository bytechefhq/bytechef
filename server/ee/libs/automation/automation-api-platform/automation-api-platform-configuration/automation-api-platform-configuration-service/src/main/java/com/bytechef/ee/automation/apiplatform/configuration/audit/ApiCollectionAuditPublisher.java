/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.audit;

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
 * Publishes audit events for API collection-lifecycle mutations.
 *
 * <p>
 * Failures must NOT propagate to callers: publishing is best-effort. If the security context cannot be resolved, the
 * principal falls back to {@code "SYSTEM"} rather than failing the surrounding business transaction.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ApiCollectionAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(ApiCollectionAuditPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public ApiCollectionAuditPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(ApiCollectionAuditEvent eventType, long apiCollectionId) {
        publish(eventType, apiCollectionId, Map.of());
    }

    public void publish(ApiCollectionAuditEvent eventType, long apiCollectionId, Map<String, Object> additionalData) {
        String principal;

        try {
            principal = SecurityUtils.fetchCurrentUserLogin()
                .orElse("SYSTEM");
        } catch (RuntimeException exception) {
            log.warn(
                "Could not resolve principal for audit event {} on api collection id={}, using SYSTEM",
                eventType, apiCollectionId, exception);

            principal = "SYSTEM";
        }

        Map<String, Object> data = new HashMap<>();

        if (additionalData != null) {
            data.putAll(additionalData);
        }

        data.putIfAbsent("apiCollectionId", String.valueOf(apiCollectionId));

        AuditEvent auditEvent = new AuditEvent(principal, eventType.name(), data);

        applicationEventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
    }
}
