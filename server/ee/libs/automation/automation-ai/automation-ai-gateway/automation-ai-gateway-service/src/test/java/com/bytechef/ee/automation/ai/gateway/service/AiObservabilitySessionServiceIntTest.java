/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class AiObservabilitySessionServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private AiObservabilitySessionService aiObservabilitySessionService;

    @Test
    public void testGetOrCreateSessionByExternalIdIsIdempotent() {
        AiObservabilitySession first = aiObservabilitySessionService.getOrCreateSessionByExternalId(
            WORKSPACE_ID, "ext-abc", null, "user-1");

        AiObservabilitySession second = aiObservabilitySessionService.getOrCreateSessionByExternalId(
            WORKSPACE_ID, "ext-abc", null, "user-2");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getExternalSessionId()).isEqualTo("ext-abc");
        // External key wins — we do not overwrite userId on subsequent calls.
        assertThat(second.getUserId()).isEqualTo("user-1");
    }

    @Test
    public void testGetOrCreateSessionByExternalIdAcceptsNumericLookingKeys() {
        // Pre-upsert fix, numeric ids were trusted blindly as internal ids. Now every call goes through the
        // external-key lookup regardless of whether the key looks like a number.
        AiObservabilitySession created = aiObservabilitySessionService.getOrCreateSessionByExternalId(
            WORKSPACE_ID, "12345", null, "user-1");

        assertThat(created.getExternalSessionId()).isEqualTo("12345");
    }
}
