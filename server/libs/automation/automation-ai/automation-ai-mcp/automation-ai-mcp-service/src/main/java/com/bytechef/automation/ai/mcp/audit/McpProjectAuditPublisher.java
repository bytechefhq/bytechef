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

package com.bytechef.automation.ai.mcp.audit;

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
 * Publishes audit events for MCP project lifecycle mutations.
 *
 * <p>
 * Publishing is best-effort: failures must NOT propagate to callers, and a missing security context falls back to a
 * {@code "SYSTEM"} principal rather than failing the surrounding business transaction.
 *
 * @author Ivica Cardic
 */
@Component
public class McpProjectAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(McpProjectAuditPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public McpProjectAuditPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(McpProjectAuditEvent eventType, long mcpProjectId) {
        publish(eventType, mcpProjectId, null);
    }

    public void publish(McpProjectAuditEvent eventType, long mcpProjectId, Map<String, Object> additionalData) {
        String principal;

        try {
            principal = SecurityUtils.fetchCurrentUserLogin()
                .orElse("SYSTEM");
        } catch (RuntimeException exception) {
            log.warn(
                "Could not resolve principal for audit event {} on MCP project id={}, using SYSTEM",
                eventType, mcpProjectId, exception);

            principal = "SYSTEM";
        }

        Map<String, Object> data = new HashMap<>();

        if (additionalData != null) {
            data.putAll(additionalData);
        }

        data.putIfAbsent("mcpProjectId", String.valueOf(mcpProjectId));

        AuditEvent auditEvent = new AuditEvent(principal, eventType.name(), data);

        applicationEventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
    }
}
