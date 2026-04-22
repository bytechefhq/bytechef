/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.RoutingPolicyListModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.RoutingPolicyModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.RoutingStrategyListModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.RoutingStrategyModel;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1")
@SuppressFBWarnings("EI")
class AiGatewayRoutingApiController implements RoutingApi {

    private static final Map<AiGatewayRoutingStrategyType, String> STRATEGY_DESCRIPTIONS = Map.of(
        AiGatewayRoutingStrategyType.SIMPLE, "Routes all requests to the first enabled deployment",
        AiGatewayRoutingStrategyType.WEIGHTED_RANDOM,
        "Distributes requests across deployments based on configured weights",
        AiGatewayRoutingStrategyType.PRIORITY_FALLBACK,
        "Routes to the highest-priority deployment, falling back to lower priorities on failure",
        AiGatewayRoutingStrategyType.COST_OPTIMIZED,
        "Routes to the deployment with the lowest input cost per million tokens",
        AiGatewayRoutingStrategyType.LATENCY_OPTIMIZED,
        "Routes to the deployment with the lowest observed latency",
        AiGatewayRoutingStrategyType.TAG_BASED,
        "Routes based on matching request tags against model properties",
        AiGatewayRoutingStrategyType.INTELLIGENT_BALANCED,
        "Balances between cost and capability based on prompt complexity",
        AiGatewayRoutingStrategyType.INTELLIGENT_COST,
        "Prefers cheaper models, escalating to capable models only for complex prompts",
        AiGatewayRoutingStrategyType.INTELLIGENT_QUALITY,
        "Prefers capable models, using cheaper models only for simple prompts");

    private final AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;

    AiGatewayRoutingApiController(AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService) {
        this.aiGatewayRoutingPolicyService = aiGatewayRoutingPolicyService;
    }

    @Override
    public ResponseEntity<RoutingStrategyListModel> listRoutingStrategies() {
        List<RoutingStrategyModel> strategyModels = Arrays.stream(AiGatewayRoutingStrategyType.values())
            .map(strategyType -> {
                RoutingStrategyModel strategyModel = new RoutingStrategyModel();

                strategyModel.setId(strategyType.name());
                strategyModel.setName(formatDisplayName(strategyType.name()));
                strategyModel.setDescription(
                    STRATEGY_DESCRIPTIONS.getOrDefault(strategyType, strategyType.name()));

                return strategyModel;
            })
            .toList();

        RoutingStrategyListModel listModel = new RoutingStrategyListModel();

        listModel.setObject("list");
        listModel.setData(strategyModels);

        return ResponseEntity.ok(listModel);
    }

    @Override
    public ResponseEntity<RoutingPolicyListModel> listRoutingPolicies() {
        List<AiGatewayRoutingPolicy> policies = aiGatewayRoutingPolicyService.getRoutingPolicies();

        List<RoutingPolicyModel> policyModels = policies.stream()
            .filter(AiGatewayRoutingPolicy::isEnabled)
            .map(policy -> {
                RoutingPolicyModel policyModel = new RoutingPolicyModel();

                policyModel.setId(String.valueOf(policy.getId()));
                policyModel.setName(policy.getName());
                policyModel.setStrategy(policy.getStrategy()
                    .name());
                policyModel.setEnabled(policy.isEnabled());

                return policyModel;
            })
            .toList();

        RoutingPolicyListModel listModel = new RoutingPolicyListModel();

        listModel.setObject("list");
        listModel.setData(policyModels);

        return ResponseEntity.ok(listModel);
    }

    private static String formatDisplayName(String enumName) {
        String[] parts = enumName.toLowerCase()
            .split("_");
        StringBuilder displayName = new StringBuilder();

        for (String part : parts) {
            if (!displayName.isEmpty()) {
                displayName.append(" ");
            }

            displayName.append(Character.toUpperCase(part.charAt(0)))
                .append(part.substring(1));
        }

        return displayName.toString();
    }
}
