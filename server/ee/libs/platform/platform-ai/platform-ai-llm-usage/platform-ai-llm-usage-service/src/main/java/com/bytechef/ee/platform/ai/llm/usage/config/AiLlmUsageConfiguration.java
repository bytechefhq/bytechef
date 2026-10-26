/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage.config;

import com.bytechef.ee.platform.ai.llm.usage.LlmCostEstimator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class AiLlmUsageConfiguration {

    @Bean
    @ConditionalOnMissingBean(LlmCostEstimator.class)
    LlmCostEstimator noopLlmCostEstimator() {
        return new NoopLlmCostEstimator();
    }
}
