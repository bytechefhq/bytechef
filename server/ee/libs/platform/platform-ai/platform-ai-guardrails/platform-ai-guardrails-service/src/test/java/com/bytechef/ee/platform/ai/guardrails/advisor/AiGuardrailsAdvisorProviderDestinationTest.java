/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.guardrails.RestorationDestination;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;

/**
 * @author Ivica Cardic
 *
 * @version ee
 */
class AiGuardrailsAdvisorProviderDestinationTest {

    private @Nullable RestorationDestination recordedDestination;

    private final AiGuardrailsAdvisorProvider provider = new AiGuardrailsAdvisorProvider() {

        @Override
        public Optional<Advisor> getAdvisor(
            @Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface,
            RestorationDestination destination) {

            recordedDestination = destination;

            return Optional.empty();
        }

        @Override
        public @Nullable SensitiveDataMetrics getMetrics(
            @Nullable PlatformType platformType, @Nullable Long jobPrincipalId, String surface) {

            return null;
        }

        @Override
        public Optional<Advisor> getAdvisorForWorkspace(
            @Nullable Long workspaceId, String surface, RestorationDestination destination) {

            recordedDestination = destination;

            return Optional.empty();
        }

        @Override
        public @Nullable SensitiveDataMetrics getMetricsForWorkspace(@Nullable Long workspaceId, String surface) {
            return null;
        }
    };

    @Test
    void testTheThreeArgOverloadResolvesAConversation() {
        provider.getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent");

        assertThat(recordedDestination).isEqualTo(RestorationDestination.CONVERSATION);
    }

    @Test
    void testAWorkflowOutputDestinationReachesTheAdvisor() {
        provider.getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent", RestorationDestination.WORKFLOW_OUTPUT);

        assertThat(recordedDestination).isEqualTo(RestorationDestination.WORKFLOW_OUTPUT);
    }
}
