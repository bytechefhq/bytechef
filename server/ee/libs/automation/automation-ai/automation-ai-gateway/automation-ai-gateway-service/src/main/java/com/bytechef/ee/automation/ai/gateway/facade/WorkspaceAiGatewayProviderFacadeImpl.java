/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.domain.ApiKey;
import com.bytechef.ee.platform.ai.gateway.dto.ProviderConnectionResult;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayUrlValidator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class WorkspaceAiGatewayProviderFacadeImpl implements WorkspaceAiGatewayProviderFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceAiGatewayProviderFacadeImpl.class);

    private final AiGatewayProviderService aiGatewayProviderService;
    private final WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;
    private final TransactionTemplate readOnlyTransactionTemplate;

    @SuppressFBWarnings("EI")
    public WorkspaceAiGatewayProviderFacadeImpl(
        AiGatewayProviderService aiGatewayProviderService,
        WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService,
        PlatformTransactionManager transactionManager) {
        this.aiGatewayProviderService = aiGatewayProviderService;
        this.workspaceAiGatewayProviderService = workspaceAiGatewayProviderService;
        this.readOnlyTransactionTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTransactionTemplate.setReadOnly(true);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
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
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteWorkspaceProvider(Long workspaceId, Long providerId) {
        verifyWorkspaceOwnership(workspaceId, providerId);

        workspaceAiGatewayProviderService.removeProviderFromWorkspace(providerId);

        aiGatewayProviderService.delete(providerId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiGatewayProvider> getWorkspaceProviders(Long workspaceId) {
        return workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
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

    @Override
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProviderConnectionResult testWorkspaceProviderConnection(Long workspaceId, Long providerId) {
        AiGatewayProvider provider;

        try {
            provider = readOnlyTransactionTemplate.execute(status -> {
                verifyWorkspaceOwnership(workspaceId, providerId);

                return aiGatewayProviderService.getProvider(providerId);
            });
        } catch (IllegalArgumentException notOwnedOrNotFound) {
            log.warn(
                "Provider connection test rejected: workspaceId={} providerId={} not owned or not found",
                workspaceId, providerId, notOwnedOrNotFound);

            return ProviderConnectionResult.failure("Provider not found");
        }

        String baseUrl = provider.getBaseUrl();

        if (baseUrl == null || baseUrl.isBlank()) {
            return ProviderConnectionResult.failure("Provider has no baseUrl configured");
        }

        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String probeUrl = trimmed + "/models";

        try {
            AiGatewayUrlValidator.validateExternalUrl(probeUrl);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn(
                "Provider connection test rejected by SSRF guard for workspaceId={} providerId={}",
                workspaceId, providerId, illegalArgumentException);

            return ProviderConnectionResult.failure("Provider baseUrl is not externally routable");
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

            log.info(
                "Provider connection test interrupted for workspaceId={} providerId={}",
                workspaceId, providerId, interruptedException);

            return ProviderConnectionResult.failure("Interrupted while contacting provider");
        } catch (Exception exception) {
            log.warn(
                "Provider connection test failed for workspaceId={} providerId={}",
                workspaceId, providerId, exception);

            return ProviderConnectionResult.failure(coarseFailureReason(exception));
        }
    }

    static String coarseFailureReason(Exception exception) {
        if (exception instanceof UnknownHostException) {
            return "Could not resolve provider host";
        }

        if (exception instanceof ConnectException) {
            return "Could not connect to provider";
        }

        if (exception instanceof HttpTimeoutException || exception instanceof SocketTimeoutException) {
            return "Provider connection timed out";
        }

        if (exception instanceof SSLException) {
            return "TLS handshake with provider failed";
        }

        return "Could not reach provider";
    }

    private void verifyWorkspaceOwnership(Long workspaceId, Long providerId) {
        boolean owned = workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId)
            .stream()
            .anyMatch(workspaceProvider -> Objects.equals(workspaceProvider.getId(), providerId));

        if (!owned) {
            throw new IllegalArgumentException(
                "Provider " + providerId + " does not belong to workspace " + workspaceId);
        }
    }

    private static void validateBaseUrlIfProvided(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return;
        }

        AiGatewayUrlValidator.validateExternalUrl(baseUrl);
    }
}
