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

package com.bytechef.component.ai.llm.tool;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.ai.tool.AiAgentToolContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer.Decision;
import com.bytechef.platform.component.rule.ComponentRuleEnforcer.ToolCall;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
public class RuleEnforcingToolCallback implements ToolCallback {

    static final String DEFERRED_RESULT =
        "{\"deferred\": true, \"reason\": \"Another tool call is awaiting approval. Retry this tool call after the " +
            "pending approval is resolved.\"}";

    private static final Logger log = LoggerFactory.getLogger(RuleEnforcingToolCallback.class);

    private final ActionContextAware actionContext;
    private final List<ClusterElement> approvalChannelClusterElements;
    private final ClusterElement clusterElement;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final Map<String, ComponentConnection> componentConnections;
    private final List<ComponentRuleEnforcer> componentRuleEnforcers;

    @Nullable
    private final Long connectionId;

    private final ToolCallback delegate;

    @SuppressFBWarnings("EI2")
    public RuleEnforcingToolCallback(
        ToolCallback delegate, ClusterElement clusterElement, List<ComponentRuleEnforcer> componentRuleEnforcers,
        @Nullable Long connectionId, ActionContext actionContext,
        List<ClusterElement> approvalChannelClusterElements, Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        this.delegate = delegate;
        this.clusterElement = clusterElement;
        this.componentRuleEnforcers = List.copyOf(componentRuleEnforcers);
        this.connectionId = connectionId;
        this.actionContext = (ActionContextAware) actionContext;
        this.approvalChannelClusterElements = List.copyOf(approvalChannelClusterElements);
        this.componentConnections = Map.copyOf(componentConnections);
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        if (componentRuleEnforcers.isEmpty()) {
            return delegate.call(toolInput, toolContext);
        }

        ToolCall toolCall = toToolCall(toolInput, toolContext);

        Decision decision = checkBeforeCall(toolCall);

        if (decision instanceof Decision.Block block) {
            return JsonUtils.write(Map.of("blocked", true, "reason", block.reason()));
        }

        if (decision instanceof Decision.RequireApproval requireApproval) {
            if (actionContext.getSuspend() != null) {
                return DEFERRED_RESULT;
            }

            return ToolApprovalRequests.raise(
                new ToolApprovalRequests.Request(
                    toolCall.toolCallName(), toolInput, requireApproval.title(), requireApproval.description(),
                    requireApproval.expiresAt(), approvalChannelClusterElements, componentConnections,
                    Map.of(
                        ToolSuspendConstants.RULE_IDS, requireApproval.ruleIds(),
                        ToolSuspendConstants.RULE_COMPONENT_NAME, toolCall.componentName(),
                        ToolSuspendConstants.RULE_TOOL_NAME, toolCall.toolName()),
                    clusterElementDefinitionService, actionContext));
        }

        String result = delegate.call(toolInput, toolContext);

        recordAfterCall(toolCall, result);

        return result;
    }

    public static void recordApprovalResolution(
        List<ComponentRuleEnforcer> componentRuleEnforcers, ToolCall toolCall, List<Long> ruleIds, boolean approved,
        @Nullable String approvedBy) {

        for (ComponentRuleEnforcer componentRuleEnforcer : componentRuleEnforcers) {
            try {
                componentRuleEnforcer.recordApprovalResolution(ruleIds, toolCall, approved, approvedBy);
            } catch (RuntimeException exception) {
                log.warn("Could not record the approval resolution for rules {}", ruleIds, exception);
            }
        }
    }

    private Decision checkBeforeCall(ToolCall toolCall) {
        for (ComponentRuleEnforcer componentRuleEnforcer : componentRuleEnforcers) {
            try {
                Decision decision = componentRuleEnforcer.checkBeforeCall(toolCall);

                if (!(decision instanceof Decision.Allow)) {
                    return decision;
                }
            } catch (RuntimeException exception) {
                log.warn(
                    "Could not evaluate component rules for tool '{}'; allowing the call",
                    toolCall.toolCallName(), exception);
            }
        }

        return new Decision.Allow();
    }

    private void recordAfterCall(ToolCall toolCall, @Nullable Object result) {
        for (ComponentRuleEnforcer componentRuleEnforcer : componentRuleEnforcers) {
            try {
                componentRuleEnforcer.recordAfterCall(toolCall, result);
            } catch (RuntimeException exception) {
                log.warn("Could not record AFTER-phase rules for tool '{}'", toolCall.toolCallName(), exception);
            }
        }
    }

    private ToolCall toToolCall(String toolInput, @Nullable ToolContext toolContext) {
        ToolDefinition toolDefinition = delegate.getToolDefinition();

        return new ToolCall(
            clusterElement.getComponentName(), clusterElement.getClusterElementName(), toolDefinition.name(),
            parseToolInput(toolInput), connectionId, actionContext.getJobId(), null,
            fetchApprovedBy(toolContext), actionContext.getJobPrincipalId(), actionContext.getPlatformType());
    }

    private static Map<String, ?> parseToolInput(String toolInput) {
        try {
            return JsonUtils.readMap(toolInput);
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    @Nullable
    private static String fetchApprovedBy(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();

        return context.get(AiAgentToolContext.APPROVED_BY_KEY) instanceof String approvedBy ? approvedBy : null;
    }
}
