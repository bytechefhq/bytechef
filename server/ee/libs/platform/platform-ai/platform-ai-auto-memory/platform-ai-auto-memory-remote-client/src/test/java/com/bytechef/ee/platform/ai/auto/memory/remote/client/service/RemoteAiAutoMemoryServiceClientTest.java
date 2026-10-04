/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.auto.memory.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 * @author Ivica Cardic
 */
class RemoteAiAutoMemoryServiceClientTest {

    private final RemoteAiAutoMemoryServiceClient remoteAiAutoMemoryServiceClient =
        new RemoteAiAutoMemoryServiceClient();

    @Test
    void testIsNotAvailable() {
        assertThat(remoteAiAutoMemoryServiceClient.isAvailable()).isFalse();
    }

    @Test
    void testStorageCallsAreUnsupported() {
        AiAutoMemoryOwner owner = new AiAutoMemoryOwner(
            1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 2L, Environment.PRODUCTION);

        assertThatThrownBy(() -> remoteAiAutoMemoryServiceClient.list(owner, null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage(RemoteAiAutoMemoryServiceClient.UNSUPPORTED_MESSAGE);
    }
}
