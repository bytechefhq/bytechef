/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage.config;

import com.bytechef.ee.platform.ai.tool.usage.AiToolUsageRecorderImpl;
import com.bytechef.ee.platform.ai.tool.usage.ToolCostEstimator;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import com.bytechef.ee.platform.ai.tool.usage.repository.AiToolUsageRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfiguration(after = AiToolUsageJdbcRepositoryConfiguration.class)
@ConditionalOnBean(AiToolUsageRepository.class)
public class AiToolUsageConfiguration {

    @Bean
    @ConditionalOnMissingBean(ToolCostEstimator.class)
    ToolCostEstimator noopToolCostEstimator() {
        return new NoopToolCostEstimator();
    }

    @Bean
    @ConditionalOnMissingBean(ToolUsageRecorder.class)
    ToolUsageRecorder aiToolUsageRecorder(
        AiToolUsageRepository repository, ToolCostEstimator costEstimator, JsonMapper jsonMapper,
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<AiToolUsageRecorderImpl> selfProvider) {

        return new AiToolUsageRecorderImpl(
            repository, costEstimator, jsonMapper, meterRegistryProvider, selfProvider);
    }
}
