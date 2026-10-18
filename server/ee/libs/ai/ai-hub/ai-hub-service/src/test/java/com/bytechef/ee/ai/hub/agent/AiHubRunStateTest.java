/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubRunStateTest {

    @Test
    void testInjectSetsEveryVerifiedKey() {
        State state = new State();

        AiHubRunState.inject(state, 3L, 7L, "thread-1", 2L, "tenant-1", 5L);

        Map<String, Object> stateMap = state.getState();

        assertThat(stateMap.get(AiHubStateKeys.AUTHENTICATED_USER_ID)).isEqualTo(3L);
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID)).isEqualTo(7L);
        assertThat(stateMap.get(AiHubStateKeys.WORKSPACE_ID)).isEqualTo(7L);
        assertThat(stateMap.get(AiHubStateKeys.USER_ID)).isEqualTo(3L);
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_THREAD_ID)).isEqualTo("thread-1");
        assertThat(stateMap.get(AiHubStateKeys.THREAD_ID)).isEqualTo("thread-1");
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID)).isEqualTo(2L);
        assertThat(stateMap.get(AiHubStateKeys.ENVIRONMENT_ID)).isEqualTo(2L);
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_TENANT_ID)).isEqualTo("tenant-1");
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_OWNER_USER_ID)).isEqualTo(5L);
    }

    @Test
    void testInjectOverwritesClientSuppliedValues() {
        State state = new State();

        state.set(AiHubStateKeys.AUTHENTICATED_USER_ID, 999L);
        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 999L);
        state.set(AiHubStateKeys.VERIFIED_THREAD_ID, "forged-thread");
        state.set(AiHubStateKeys.THREAD_ID, "forged-thread");

        AiHubRunState.inject(state, 3L, 7L, "thread-1", 0L, "tenant-1", 3L);

        Map<String, Object> stateMap = state.getState();

        assertThat(stateMap.get(AiHubStateKeys.AUTHENTICATED_USER_ID)).isEqualTo(3L);
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID)).isEqualTo(7L);
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_THREAD_ID)).isEqualTo("thread-1");
        assertThat(stateMap.get(AiHubStateKeys.THREAD_ID)).isEqualTo("thread-1");
    }

    @Test
    void testClampEnvironmentIdReturnsValueWithinRange() {
        assertThat(AiHubRunState.clampEnvironmentId(1L)).isEqualTo(1L);
    }

    @Test
    void testClampEnvironmentIdFallsBackToZeroWhenNull() {
        assertThat(AiHubRunState.clampEnvironmentId(null)).isEqualTo(0L);
    }

    @Test
    void testClampEnvironmentIdFallsBackToZeroWhenNegative() {
        assertThat(AiHubRunState.clampEnvironmentId(-1L)).isEqualTo(0L);
    }

    @Test
    void testClampEnvironmentIdFallsBackToZeroWhenOutOfRange() {
        assertThat(AiHubRunState.clampEnvironmentId(999L)).isEqualTo(0L);
    }
}
