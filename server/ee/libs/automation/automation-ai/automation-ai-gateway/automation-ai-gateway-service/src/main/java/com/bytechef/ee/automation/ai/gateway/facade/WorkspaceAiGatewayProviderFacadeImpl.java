/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.automation.ai.gateway.domain.ApiKey;
import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.dto.ProviderConnectionResult;
import com.bytechef.ee.automation.ai.gateway.security.AiGatewayUrlValidator;
import com.bytechef.ee.automation.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceAiGatewayProviderFacade} interface that handles workspace AI LLM Gateway
 * provider operations.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class WorkspaceAiGatewayProviderFacadeImpl implements WorkspaceAiGatewayProviderFacade {

    private final AiGatewayProviderService aiGatewayProviderService;
    private final WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;

    @SuppressFBWarnings("EI")
    public WorkspaceAiGatewayProviderFacadeImpl(
        AiGatewayProviderService aiGatewayProviderService,
        WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService) {

        this.aiGatewayProviderService = aiGatewayProviderService;
        this.workspaceAiGatewayProviderService = workspaceAiGatewayProviderService;
    }

    @Override
    public AiGatewayProvider createWorkspaceProvider(
        String name, AiGatewayProviderType type, String apiKey, String baseUrl, String config, Long workspaceId) {

        validateBaseUrlIfProvided(baseUrl);

        AiGatewayProvider provider = new AiGatewayProvider(name, type, apiKey);

        provider.setBaseUrl(baseUrl);
        provider.setConfig(config);

        provider = aiGatewayProviderService.create(provider);

        workspaceAiGatewayProviderService.assignProviderToWorkspace(provider.getId(), workspaceId);

        return provider;
    }

    @Override
    public void deleteWorkspaceProvider(Long workspaceId, Long providerId) {
        verifyWorkspaceOwnership(workspaceId, providerId);

        workspaceAiGatewayProviderService.removeProviderFromWorkspace(providerId);

        aiGatewayProviderService.delete(providerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiGatewayProvider> getWorkspaceProviders(Long workspaceId) {
        List<Long> providerIds = workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId)
            .stream()
            .map(WorkspaceAiGatewayProvider::getProviderId)
            .toList();

        if (providerIds.isEmpty()) {
            return List.of();
        }

        return aiGatewayProviderService.getProviders(providerIds);
    }

    @Override
    public AiGatewayProvider updateWorkspaceProvider(
        Long workspaceId, Long id, String name, String apiKey, String baseUrl, Boolean enabled, String config) {

        verifyWorkspaceOwnership(workspaceId, id);

        validateBaseUrlIfProvided(baseUrl);

        AiGatewayProvider provider = aiGatewayProviderService.getProvider(id);

        provider.setName(name);

        if (apiKey != null) {
            provider.setApiKey(ApiKey.of(apiKey));
        }

        provider.setBaseUrl(baseUrl);

        if (enabled != null) {
            provider.setEnabled(enabled);
        }

        provider.setConfig(config);

        return aiGatewayProviderService.update(provider);
    }

    /**
     * Probes a provider's connectivity. Resolves the endpoint via provider {@code baseUrl} or the provider-type
     * default, sends a short-timeout GET to the shared {@code /models} endpoint (most OpenAI-compatible providers), and
     * returns a structured pass/fail result. Intentionally catches all exceptions so UI callers always see a
     * {@link ProviderConnectionResult} and not a 500.
     */
    @Override
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @Transactional(readOnly = true)
    public ProviderConnectionResult testWorkspaceProviderConnection(Long workspaceId, Long providerId) {
        verifyWorkspaceOwnership(workspaceId, providerId);

        AiGatewayProvider provider = aiGatewayProviderService.getProvider(providerId);

        String baseUrl = provider.getBaseUrl();

        if (baseUrl == null || baseUrl.isBlank()) {
            return ProviderConnectionResult.failure("Provider has no baseUrl configured");
        }

        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String probeUrl = trimmed + "/models";

        // SSRF guard: validate the base URL resolves to a public address before sending the Authorization header.
        // Without this, a workspace admin could point baseUrl at 169.254.169.254 or other internal services and
        // exfiltrate the api key via Authorization: Bearer.
        try {
            AiGatewayUrlValidator.validateExternalUrl(probeUrl);
        } catch (IllegalArgumentException illegalArgumentException) {
            return ProviderConnectionResult.failure(
                "Provider baseUrl rejected: " + illegalArgumentException.getMessage());
        }

        long startNs = System.nanoTime();

        try {
            HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(probeUrl))
                .timeout(Duration.ofSeconds(10))
                .GET();

            String apiKey = provider.revealApiKey();

            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<Void> response =
                httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding());

            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;

            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                return ProviderConnectionResult.success(latencyMs);
            }

            return ProviderConnectionResult.failure("Provider returned HTTP " + statusCode);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            return ProviderConnectionResult.failure("Interrupted while contacting provider");
        } catch (Exception exception) {
            return ProviderConnectionResult.failure(
                "Failed to reach provider: " + exception.getClass()
                    .getSimpleName() + ": " + exception.getMessage());
        }
    }

    private void verifyWorkspaceOwnership(Long workspaceId, Long providerId) {
        boolean owned = workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId)
            .stream()
            .anyMatch(workspaceProvider -> workspaceProvider.getProviderId()
                .equals(providerId));

        if (!owned) {
            throw new IllegalArgumentException(
                "Provider " + providerId + " does not belong to workspace " + workspaceId);
        }
    }

    /**
     * SSRF guard at write time: a workspace admin must not be able to set {@code baseUrl} to a loopback, private,
     * link-local, CGNAT or IPv6 ULA target. Without this check, {@code AiGatewayChatModelFactory} would later send
     * {@code Authorization: Bearer <apiKey>} to the internal target on every chat completion, exfiltrating the api key.
     *
     * <p>
     * The same validator also runs inside {@code AiGatewayChatModelFactory} as defense-in-depth, but validating at
     * write time gives a fast caller-facing error instead of a delayed runtime failure.
     */
    private static void validateBaseUrlIfProvided(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return;
        }

        AiGatewayUrlValidator.validateExternalUrl(baseUrl);
    }
}
