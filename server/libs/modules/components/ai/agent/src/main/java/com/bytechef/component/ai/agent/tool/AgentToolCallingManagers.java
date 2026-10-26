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

package com.bytechef.component.ai.agent.tool;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenBoundaryToolCallingManager;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallLimitBehavior;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public final class AgentToolCallingManagers {

    private final ToolCallingManager defaultToolCallingManager;
    private final ObservationRegistry observationRegistry;
    private final SensitiveDataRedactor sensitiveDataRedactor;
    private final @Nullable ToolCallbackResolver toolCallbackResolver;
    private final @Nullable ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

    public AgentToolCallingManagers(ToolCallingManager defaultToolCallingManager) {
        this.defaultToolCallingManager = defaultToolCallingManager;
        this.observationRegistry = ObservationRegistry.NOOP;
        this.sensitiveDataRedactor = new SensitiveDataRedactor(List.of());
        this.toolCallbackResolver = null;
        this.toolExecutionExceptionProcessor = null;
    }

    @Autowired
    public AgentToolCallingManagers(
        ToolCallingManager defaultToolCallingManager,
        ObjectProvider<SensitiveDataRedactor> sensitiveDataRedactorProvider,
        ObjectProvider<ToolCallbackResolver> toolCallbackResolverProvider,
        ObjectProvider<ToolExecutionExceptionProcessor> toolExecutionExceptionProcessorProvider,
        ObjectProvider<ObservationRegistry> observationRegistryProvider) {

        this.defaultToolCallingManager = defaultToolCallingManager;
        this.observationRegistry = observationRegistryProvider.getIfAvailable(() -> ObservationRegistry.NOOP);
        this.sensitiveDataRedactor =
            sensitiveDataRedactorProvider.getIfAvailable(() -> new SensitiveDataRedactor(List.of()));
        this.toolCallbackResolver = toolCallbackResolverProvider.getIfAvailable();
        this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessorProvider.getIfAvailable();
    }

    public ToolCallingManager getToolCallingManager(@Nullable SensitiveDataMetrics metrics) {
        return getToolCallingManager(metrics, null);
    }

    public ToolCallingManager getToolCallingManager(
        @Nullable SensitiveDataMetrics metrics, @Nullable Integer maxToolCalls) {

        return PiiTokenBoundaryToolCallingManager.wrap(
            getLimitedToolCallingManager(maxToolCalls), sensitiveDataRedactor, () -> metrics);
    }

    private ToolCallingManager getLimitedToolCallingManager(@Nullable Integer maxToolCalls) {
        if (maxToolCalls == null || toolCallbackResolver == null || toolExecutionExceptionProcessor == null) {
            return defaultToolCallingManager;
        }

        return DefaultToolCallingManager.builder()
            .observationRegistry(observationRegistry)
            .toolCallbackResolver(toolCallbackResolver)
            .toolExecutionExceptionProcessor(toolExecutionExceptionProcessor)
            .unlimitedCallsPerTool()
            .maxTotalToolCalls(maxToolCalls)
            .onLimitExceeded(ToolCallLimitBehavior.RETURN_ERROR_RESPONSE)
            .build();
    }
}
