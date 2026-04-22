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

package com.bytechef.ee.platform.ai.tool.usage.config;

import com.bytechef.ee.platform.ai.tool.usage.AiToolUsageRecorderImpl;
import com.bytechef.ee.platform.ai.tool.usage.ToolCostEstimator;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import com.bytechef.ee.platform.ai.tool.usage.repository.AiToolUsageRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bean wiring for the shared AI tool-usage recorder. Picked up via the host application's component scan (every server
 * app declares {@code scanBasePackages = "com.bytechef"}). Registers the default {@link AiToolUsageRecorderImpl} and a
 * {@link NoopToolCostEstimator} fallback unless an agent surface (e.g. AI Hub) has already provided its own bean for
 * either contract.
 *
 * <p>
 * Kept as a plain {@code @Configuration} (not an {@code @AutoConfiguration}) so override semantics match the rest of
 * the platform — agent surfaces register their own beans with regular Spring annotations and the
 * {@code @ConditionalOnMissingBean} guards here defer to them. Persistence wiring (entity mapping, repository scanning)
 * lives in the sibling {@link AiToolUsageJdbcRepositoryConfiguration}, which still uses {@code @AutoConfiguration}
 * because Spring Data's repository scan is best activated through the auto-config lifecycle.
 * </p>
 *
 * @author Ivica Cardic
 */
@Configuration
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
        AiToolUsageRepository repository,
        com.bytechef.ee.platform.ai.tool.usage.repository.WorkspaceAiToolUsageRepository workspaceRepository,
        ToolCostEstimator costEstimator, JsonMapper jsonMapper,
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        ObjectProvider<AiToolUsageRecorderImpl> selfProvider) {

        return new AiToolUsageRecorderImpl(
            repository, workspaceRepository, costEstimator, jsonMapper, meterRegistryProvider, selfProvider);
    }
}
