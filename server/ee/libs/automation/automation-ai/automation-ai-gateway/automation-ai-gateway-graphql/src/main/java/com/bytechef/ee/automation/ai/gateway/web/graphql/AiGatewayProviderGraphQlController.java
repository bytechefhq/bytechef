/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.automation.ai.gateway.domain.ApiKey;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing AI LLM Gateway providers.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayProviderGraphQlController {

    private final AiGatewayProviderService aiGatewayProviderService;

    @SuppressFBWarnings("EI")
    AiGatewayProviderGraphQlController(AiGatewayProviderService aiGatewayProviderService) {
        this.aiGatewayProviderService = aiGatewayProviderService;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProvider aiGatewayProvider(@Argument long id) {
        return aiGatewayProviderService.getProvider(id);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiGatewayProvider> aiGatewayProviders() {
        return aiGatewayProviderService.getProviders();
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProvider createAiGatewayProvider(@Argument CreateAiGatewayProviderInput input) {
        AiGatewayProvider provider = new AiGatewayProvider(input.name(), input.type(), input.apiKey());

        provider.setBaseUrl(input.baseUrl());
        provider.setConfig(validateJsonConfig(input.config()));

        return aiGatewayProviderService.create(provider);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public boolean deleteAiGatewayProvider(@Argument long id) {
        aiGatewayProviderService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayProvider updateAiGatewayProvider(
        @Argument long id, @Argument UpdateAiGatewayProviderInput input) {

        AiGatewayProvider provider = aiGatewayProviderService.getProvider(id);

        provider.setName(input.name());

        if (input.apiKey() != null) {
            provider.setApiKey(ApiKey.of(input.apiKey()));
        }

        provider.setBaseUrl(input.baseUrl());

        if (input.enabled() != null) {
            provider.setEnabled(input.enabled());
        }

        provider.setConfig(validateJsonConfig(input.config()));

        return aiGatewayProviderService.update(provider);
    }

    @SuppressFBWarnings("EI")
    public record CreateAiGatewayProviderInput(
        String name, AiGatewayProviderType type, String apiKey, String baseUrl, String config) {
    }

    @SuppressFBWarnings("EI")
    public record UpdateAiGatewayProviderInput(
        String name, AiGatewayProviderType type, String apiKey, String baseUrl, Boolean enabled, String config) {
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
