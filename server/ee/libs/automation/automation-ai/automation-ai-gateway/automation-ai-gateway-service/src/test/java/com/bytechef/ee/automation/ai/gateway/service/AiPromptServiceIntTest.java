/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.gateway.config.AiGatewayIntTestConfiguration;
import com.bytechef.ee.automation.ai.gateway.domain.AiPrompt;
import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersion;
import com.bytechef.ee.automation.ai.gateway.domain.AiPromptVersionType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
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
public class AiPromptServiceIntTest {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private AiPromptService aiPromptService;

    @Autowired
    private AiPromptVersionService aiPromptVersionService;

    @Test
    public void testCreatePromptAndVersionsWithEnvironments() {
        AiPrompt prompt = aiPromptService.create(new AiPrompt(WORKSPACE_ID, "welcome-prompt"));

        Long promptId = Validate.notNull(prompt.getId(), "id");

        int v1Number = aiPromptVersionService.getNextVersionNumber(promptId);

        assertThat(v1Number).isEqualTo(1);

        AiPromptVersion v1 = aiPromptVersionService.create(
            new AiPromptVersion(promptId, v1Number, AiPromptVersionType.TEXT, "Hello v1", "tester"));

        int v2Number = aiPromptVersionService.getNextVersionNumber(promptId);

        assertThat(v2Number).isEqualTo(2);

        AiPromptVersion v2 = aiPromptVersionService.create(
            new AiPromptVersion(promptId, v2Number, AiPromptVersionType.TEXT, "Hello v2", "tester"));

        aiPromptVersionService.setActiveVersion(Validate.notNull(v1.getId(), "id"), "production");
        aiPromptVersionService.setActiveVersion(Validate.notNull(v2.getId(), "id"), "staging");

        Optional<AiPromptVersion> production = aiPromptVersionService.getActiveVersion(promptId, "production");
        Optional<AiPromptVersion> staging = aiPromptVersionService.getActiveVersion(promptId, "staging");

        assertThat(production).isPresent();
        assertThat(production.get()
            .getContent()).isEqualTo("Hello v1");

        assertThat(staging).isPresent();
        assertThat(staging.get()
            .getContent()).isEqualTo("Hello v2");
    }
}
