/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.service.AiLlmUsageService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGatewayIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@AiGatewayIntTestConfigurationSharedMocks
public class CrossWorkspaceIsolationIntTest {

    private static final Long WORKSPACE_A = 101L;
    private static final Long WORKSPACE_B = 202L;

    @Autowired
    private AiLlmUsageService aiGatewayRequestLogService;

    @Test
    void testDeleteOlderThanByWorkspaceOnRequestLogIsScoped() {
        AiLlmUsage logA = new AiLlmUsage("req-A", "openai/gpt-4");
        logA.setStatus(200);
        logA.setCost(BigDecimal.ZERO);
        aiGatewayRequestLogService.create(logA, WORKSPACE_A);

        AiLlmUsage logB = new AiLlmUsage("req-B", "openai/gpt-4");
        logB.setStatus(200);
        logB.setCost(BigDecimal.ZERO);
        aiGatewayRequestLogService.create(logB, WORKSPACE_B);

        aiGatewayRequestLogService.deleteOlderThanByWorkspace(Instant.now(), WORKSPACE_A);

        List<AiLlmUsage> remainingB = aiGatewayRequestLogService.getRequestLogsByWorkspace(
            WORKSPACE_B, Instant.now()
                .minus(1, ChronoUnit.DAYS),
            Instant.now()
                .plus(1, ChronoUnit.DAYS));

        assertThat(remainingB)
            .as("Scoped request-log delete on workspace A must not affect workspace B")
            .hasSize(1);
    }
}
