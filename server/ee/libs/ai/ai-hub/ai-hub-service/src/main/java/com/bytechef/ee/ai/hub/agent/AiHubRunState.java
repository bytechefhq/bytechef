/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.platform.configuration.domain.Environment;
import org.jspecify.annotations.Nullable;

/**
 * Writes the server-verified identity values ({@code userId}, {@code workspaceId}, {@code threadId}, the chat owner,
 * {@code environmentId}, {@code tenantId}) into the reserved {@link AiHubStateKeys} on an AG-UI {@link State}. Shared
 * by {@code AiHubApiController} (a user's turn) and {@link AiHubContinuationStarterImpl} (the turn an approval
 * resolution starts), so both entry points stamp the same server-controlled keys the agent and its tool callbacks
 * trust.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubRunState {

    private AiHubRunState() {
    }

    /**
     * Writes the verified identity values into {@code state}, overwriting whatever the client sent under the same keys.
     * The plain key aliases are overwritten with the same verified values as their {@code VERIFIED_*} counterparts, so
     * code reading {@code state.workspaceId} or {@code state.userId} directly still gets server-controlled data.
     */
    public static void inject(
        State state, long userId, long workspaceId, String threadId, long environmentId, String tenantId,
        long ownerUserId) {

        state.set(AiHubStateKeys.AUTHENTICATED_USER_ID, userId);
        state.set(AiHubStateKeys.USER_ID, userId);
        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, workspaceId);
        state.set(AiHubStateKeys.WORKSPACE_ID, workspaceId);
        state.set(AiHubStateKeys.VERIFIED_THREAD_ID, threadId);
        state.set(AiHubStateKeys.THREAD_ID, threadId);
        state.set(AiHubStateKeys.VERIFIED_OWNER_USER_ID, ownerUserId);
        state.set(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID, environmentId);
        state.set(AiHubStateKeys.ENVIRONMENT_ID, environmentId);
        state.set(AiHubStateKeys.VERIFIED_TENANT_ID, tenantId);
    }

    /**
     * Range-checks a client-supplied environment ordinal, falling back to {@code 0} (DEVELOPMENT) when it is missing or
     * out of range for {@link Environment}.
     */
    public static long clampEnvironmentId(@Nullable Long rawEnvironmentId) {
        return rawEnvironmentId != null && rawEnvironmentId >= 0 && rawEnvironmentId < Environment.values().length
            ? rawEnvironmentId
            : 0L;
    }
}
