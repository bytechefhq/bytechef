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

import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallLimitBehavior;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Supplies the {@link ToolCallingManager} an agent run should use.
 *
 * <p>
 * Spring AI's tool call limits ({@code ToolCallLimits}, 2.0.1+) are fixed when the manager is built and are not
 * expressible per request - {@code ToolCallingChatOptions} carries no limit, and the only public configuration surface
 * besides the builder is the application-wide {@code spring.ai.tools.limits.*} properties. A per-agent cap therefore
 * needs its own manager, which is why the application's collaborators are collected here rather than left implicit:
 * building one from {@code DefaultToolCallingManager.builder()} defaults would quietly substitute a different callback
 * resolver and, more damagingly, a different {@link ToolExecutionExceptionProcessor}, changing whether a failing tool
 * is reported back to the model or thrown.
 *
 * <p>
 * Existing as a bean keeps this out of {@code AiAgentComponentHandler}'s constructor, which already telescopes through
 * seven parameters.
 *
 * @author Ivica Cardic
 */
@Component
public final class AgentToolCallingManagers {

    private final ToolCallingManager defaultToolCallingManager;
    private final @Nullable ToolCallbackResolver toolCallbackResolver;
    private final @Nullable ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;
    private final ObservationRegistry observationRegistry;

    /**
     * The application's manager alone, with no ability to build a limited variant of it — a configured
     * {@code maxToolCalls} is then honoured as "no cap" rather than enforced. For callers that assemble an agent
     * outside a Spring context.
     */
    public AgentToolCallingManagers(ToolCallingManager defaultToolCallingManager) {
        this.defaultToolCallingManager = defaultToolCallingManager;
        this.toolCallbackResolver = null;
        this.toolExecutionExceptionProcessor = null;
        this.observationRegistry = ObservationRegistry.NOOP;
    }

    public AgentToolCallingManagers(
        ToolCallingManager defaultToolCallingManager, ObjectProvider<ToolCallbackResolver> toolCallbackResolverProvider,
        ObjectProvider<ToolExecutionExceptionProcessor> toolExecutionExceptionProcessorProvider,
        ObjectProvider<ObservationRegistry> observationRegistryProvider) {

        this.defaultToolCallingManager = defaultToolCallingManager;
        this.toolCallbackResolver = toolCallbackResolverProvider.getIfAvailable();
        this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessorProvider.getIfAvailable();
        this.observationRegistry = observationRegistryProvider.getIfAvailable(() -> ObservationRegistry.NOOP);
    }

    /**
     * The application's own manager when {@code maxToolCalls} is unset, otherwise a copy of it carrying that total
     * limit. The per-tool cap is lifted on the copy ({@code DEFAULT_MAX_CALLS_PER_TOOL} is 40) so that the one number
     * the agent configures is the only one that can stop it - a total of 100 that silently became 40 for the tool the
     * agent actually leans on would be indistinguishable from the agent giving up early.
     *
     * <p>
     * Falls back to the application's manager, uncapped, if either collaborator is missing rather than rebuilding from
     * defaults; an unenforced limit is a smaller surprise than tool errors changing shape.
     */
    public ToolCallingManager getToolCallingManager(@Nullable Integer maxToolCalls) {
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
