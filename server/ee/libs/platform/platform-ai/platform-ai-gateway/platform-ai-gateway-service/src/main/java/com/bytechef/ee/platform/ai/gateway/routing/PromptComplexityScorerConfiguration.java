/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class PromptComplexityScorerConfiguration {

    @Bean
    @ConditionalOnProperty(
        prefix = "bytechef.ai.gateway", name = "prompt-complexity-scorer", havingValue = "deterministic",
        matchIfMissing = true)
    PromptComplexityScorer deterministicPromptComplexityScorer() {
        return new DeterministicPromptComplexityScorer();
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "bytechef.ai.gateway", name = "prompt-complexity-scorer", havingValue = "opennlp")
    PromptComplexityScorer openNlpPromptComplexityScorer() {
        return new OpenNlpPromptComplexityScorer();
    }
}
