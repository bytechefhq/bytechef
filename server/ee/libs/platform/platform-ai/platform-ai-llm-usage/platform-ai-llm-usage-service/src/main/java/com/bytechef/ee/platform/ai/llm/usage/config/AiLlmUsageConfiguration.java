/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.platform.ai.llm.usage.config;

import com.bytechef.ee.platform.ai.llm.usage.LlmCostEstimator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean wiring for the shared AI LLM-usage cost-estimator fallback. The
 * {@link com.bytechef.ee.platform.ai.llm.usage.LlmUsageRecorder} bean is satisfied by {@code AiLlmUsageServiceImpl}
 * (registered via {@code @Service}), so the only thing this config does is supply a {@link NoopLlmCostEstimator} for
 * deployments without a configured rate sheet. Agent surfaces with a concrete rate table register their own
 * {@link LlmCostEstimator} bean to override.
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
