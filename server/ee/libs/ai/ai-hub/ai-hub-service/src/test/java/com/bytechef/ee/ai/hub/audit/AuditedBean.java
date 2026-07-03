/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.audit;

import org.springframework.stereotype.Component;

/**
 * Stub component used by {@link AiHubAuditAspectIntTest} to exercise the aspect-driven audit path.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class AuditedBean {

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_UPDATED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#agentId")
        })
    public void simulateUpdate(long workspaceId, long agentId) {
    }
}
