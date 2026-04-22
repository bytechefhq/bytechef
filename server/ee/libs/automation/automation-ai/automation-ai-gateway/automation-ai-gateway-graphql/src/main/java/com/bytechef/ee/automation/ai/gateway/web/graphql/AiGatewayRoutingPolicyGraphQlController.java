/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicy;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingPolicyTag;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRoutingStrategyType;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayModelDeploymentService;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayRoutingPolicyService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing AI LLM Gateway routing policies.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayRoutingPolicyGraphQlController {

    private final AiGatewayModelDeploymentService aiGatewayModelDeploymentService;
    private final AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService;

    @SuppressFBWarnings("EI")
    AiGatewayRoutingPolicyGraphQlController(
        AiGatewayModelDeploymentService aiGatewayModelDeploymentService,
        AiGatewayRoutingPolicyService aiGatewayRoutingPolicyService) {

        this.aiGatewayModelDeploymentService = aiGatewayModelDeploymentService;
        this.aiGatewayRoutingPolicyService = aiGatewayRoutingPolicyService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRoutingPolicy aiGatewayRoutingPolicy(@Argument long id) {
        return aiGatewayRoutingPolicyService.getRoutingPolicy(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiGatewayRoutingPolicy> aiGatewayRoutingPolicies() {
        return aiGatewayRoutingPolicyService.getRoutingPolicies();
    }

    @SchemaMapping(typeName = "AiGatewayRoutingPolicy", field = "deployments")
    public List<AiGatewayModelDeployment> deployments(AiGatewayRoutingPolicy routingPolicy) {
        return aiGatewayModelDeploymentService.getDeploymentsByRoutingPolicyId(routingPolicy.getId());
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRoutingPolicy createAiGatewayRoutingPolicy(
        @Argument CreateAiGatewayRoutingPolicyInput input) {

        AiGatewayRoutingPolicy policy = new AiGatewayRoutingPolicy(input.name(), input.strategy());

        policy.setFallbackModel(input.fallbackModel());
        policy.setConfig(validateJsonConfig(input.config()));
        policy.setTags(toAiGatewayRoutingPolicyTags(input.tagIds()));

        return aiGatewayRoutingPolicyService.create(policy);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiGatewayRoutingPolicy(@Argument long id) {
        aiGatewayRoutingPolicyService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayRoutingPolicy updateAiGatewayRoutingPolicy(
        @Argument long id, @Argument UpdateAiGatewayRoutingPolicyInput input) {

        AiGatewayRoutingPolicy policy = aiGatewayRoutingPolicyService.getRoutingPolicy(id);

        if (input.name() != null) {
            policy.setName(input.name());
        }

        if (input.strategy() != null) {
            policy.setStrategy(input.strategy());
        }

        if (input.fallbackModel() != null) {
            policy.setFallbackModel(input.fallbackModel());
        }

        if (input.config() != null) {
            policy.setConfig(validateJsonConfig(input.config()));
        }

        if (input.enabled() != null) {
            policy.setEnabled(input.enabled());
        }

        if (input.tagIds() != null) {
            policy.setTags(toAiGatewayRoutingPolicyTags(input.tagIds()));
        }

        return aiGatewayRoutingPolicyService.update(policy);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiGatewayRoutingPolicyInput(
        String name, AiGatewayRoutingStrategyType strategy, String fallbackModel, String config,
        List<String> tagIds) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiGatewayRoutingPolicyInput(
        String name, AiGatewayRoutingStrategyType strategy, String fallbackModel, String config, Boolean enabled,
        List<String> tagIds) {
    }

    private static Set<AiGatewayRoutingPolicyTag> toAiGatewayRoutingPolicyTags(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }

        return tagIds.stream()
            .map(tagId -> new AiGatewayRoutingPolicyTag(Long.parseLong(tagId)))
            .collect(Collectors.toSet());
    }

    private static String validateJsonConfig(String config) {
        if (config != null && !config.isBlank()) {
            try {
                JsonUtils.readTree(config);
            } catch (Exception exception) {
                throw new IllegalArgumentException("config must be valid JSON: " + exception.getMessage());
            }
        }

        return config;
    }
}
