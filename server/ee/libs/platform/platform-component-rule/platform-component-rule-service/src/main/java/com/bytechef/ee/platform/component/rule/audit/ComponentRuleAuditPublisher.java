/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.audit;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ComponentRuleAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(ComponentRuleAuditPublisher.class);

    public record ComponentRuleAuditPayload(
        long ruleId, String componentName, String toolName, @Nullable String toolCallName, String phase,
        @Nullable Long jobId, @Nullable Long taskExecutionId, @Nullable String approvedBy,
        @Nullable String wouldHave, boolean strictFallback) {

        Map<String, Object> toMap() {
            Map<String, Object> data = new HashMap<>();

            data.put("ruleId", String.valueOf(ruleId));
            data.put("componentName", componentName);
            data.put("toolName", toolName);
            data.put("phase", phase);

            if (toolCallName != null) {
                data.put("toolCallName", toolCallName);
            }

            if (jobId != null) {
                data.put("jobId", String.valueOf(jobId));
            }

            if (taskExecutionId != null) {
                data.put("taskExecutionId", String.valueOf(taskExecutionId));
            }

            if (approvedBy != null) {
                data.put("approvedBy", approvedBy);
            }

            if (wouldHave != null) {
                data.put("wouldHave", wouldHave);
            }

            if (strictFallback) {
                data.put("strictFallback", true);
            }

            return data;
        }
    }

    private final ApplicationEventPublisher applicationEventPublisher;

    public ComponentRuleAuditPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(ComponentRuleAuditEvent componentRuleAuditEvent, ComponentRuleAuditPayload payload) {
        try {
            String principal = SecurityUtils.fetchCurrentUserLogin()
                .orElse("SYSTEM");

            AuditEvent auditEvent = new AuditEvent(principal, componentRuleAuditEvent.name(), payload.toMap());

            applicationEventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
        } catch (RuntimeException exception) {
            log.warn(
                "Could not publish audit event {} for component rule id={}", componentRuleAuditEvent,
                payload.ruleId(), exception);
        }
    }
}
