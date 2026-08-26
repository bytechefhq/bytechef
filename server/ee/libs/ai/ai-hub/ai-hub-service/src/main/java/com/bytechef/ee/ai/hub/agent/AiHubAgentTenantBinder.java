/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.tenant.TenantContext;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binds {@link TenantContext} around an AI Hub agent run from the tenant id {@code AiHubApiController} captured on the
 * request thread into {@link AiHubStateKeys#VERIFIED_TENANT_ID}.
 *
 * <p>
 * {@code LocalAgent.runAgent} hands every agent body to a bare {@code CompletableFuture.runAsync} with no executor
 * argument, so it runs on a {@code ForkJoinPool.commonPool()} worker that inherits no thread-local.
 * {@code TenantContext} initialises to {@code DEFAULT_TENANT_ID}, so without this an agent body silently reads the
 * {@code public} schema. Hoisting the binding into {@code AiHubRoutingAgent.runAgent} does not work and should not be
 * retried: that method returns before the async hop, so it is still on the request thread.
 *
 * <p>
 * Shared by {@link AiHubSpringAIAgent} and {@link WebhookBridgeAgent}, which have no common base — the first extends
 * {@code SpringAIAgent} and the second {@code LocalAgent} directly — but the identical defect.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AiHubAgentTenantBinder {

    private static final Logger log = LoggerFactory.getLogger(AiHubAgentTenantBinder.class);

    private AiHubAgentTenantBinder() {
    }

    /**
     * Runs {@code action} with the tenant carried in {@code state} bound, restoring the thread's previous tenant in a
     * {@code finally}. The common pool is shared with unrelated work, so a leaked binding would contaminate whatever
     * runs next on that thread; restoring the previous value rather than clearing also keeps this re-entrant with
     * {@code RehydrateContextToolCallback}, which performs the same save/set/restore around each tool call further down
     * the stack.
     *
     * <p>
     * An absent key leaves the ambient tenant untouched and logs, rather than binding
     * {@code TenantContext.DEFAULT_TENANT_ID} over it — that would fix nothing and would make a producer that failed to
     * capture the tenant indistinguishable from one that captured it correctly.
     *
     * <p>
     * Deliberately not {@link TenantContext#runWithTenantId}, whose shape is otherwise identical: that helper rethrows
     * everything the body throws wrapped in a {@code RuntimeException}, which would replace each failure's
     * client-visible RUN_ERROR text with "Unable to execute run with tenant ID …".
     */
    static void runWithTenant(String agentId, @Nullable State state, Runnable action) {
        String tenantId = fetchTenantId(state);

        if (tenantId == null) {
            log.warn(
                "Agent '{}': turn state carries no '{}'; leaving the agent thread on the ambient tenant '{}'.",
                agentId, AiHubStateKeys.VERIFIED_TENANT_ID, TenantContext.getCurrentTenantId());

            action.run();

            return;
        }

        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            action.run();
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    private static @Nullable String fetchTenantId(@Nullable State state) {
        Object value = state == null ? null : state.get(AiHubStateKeys.VERIFIED_TENANT_ID);

        if (value == null) {
            return null;
        }

        String tenantId = value.toString();

        if (tenantId.isBlank()) {
            return null;
        }

        return tenantId;
    }
}
