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
import com.bytechef.ee.platform.ai.observability.security.AiObservabilityUrlValidator;
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
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
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
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public void deleteWorkspaceProvider(Long workspaceId, Long providerId) {
        verifyWorkspaceOwnership(workspaceId, providerId);

        workspaceAiGatewayProviderService.removeProviderFromWorkspace(providerId);

        aiGatewayProviderService.delete(providerId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    public List<AiGatewayProvider> getWorkspaceProviders(Long workspaceId) {
        return workspaceAiGatewayProviderService.getWorkspaceProviders(workspaceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
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
     *
     * <p>
     * Annotated with {@code @Transactional(propagation = NOT_SUPPORTED)} to suspend any inherited class-level
     * transaction: the HTTP probe below can hold for up to 15 seconds (5s connect + 10s request) and a JDBC connection
     * held across that window — as a class-level {@code @Transactional} or method-level
     * {@code @Transactional(readOnly = true)} would do — exhausts the read-only pool under load. The two short DB reads
     * (workspace-ownership check + provider snapshot) run inside an explicit
     * {@code readOnlyTransactionTemplate.execute(...)} block instead so the connection is released before the probe
     * begins. {@code TransactionTemplate} is required (not a self-call to a {@code @Transactional} method) because
     * Spring AOP does not proxy intra-class calls.
     *
     * <p>
     * The snapshot block is wrapped in its own catch so a wrong-workspace ownership failure produces a coarse
     * {@link ProviderConnectionResult#failure} instead of leaking the raw {@code IllegalArgumentException} message
     * ("Provider 7 does not belong to workspace 42") through the global exception handler — that path would expose
     * cross-workspace provider existence and break the "always returns a structured result" UI contract documented
     * above.
     */
    @Override
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.USER + "\")")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProviderConnectionResult testWorkspaceProviderConnection(Long workspaceId, Long providerId) {
        AiGatewayProvider provider;

        try {
            provider = readOnlyTransactionTemplate.execute(status -> {
                verifyWorkspaceOwnership(workspaceId, providerId);

                return aiGatewayProviderService.getProvider(providerId);
            });
        } catch (IllegalArgumentException notOwnedOrNotFound) {
            // WARN: a workspace admin testing a providerId they do not own (or that no longer exists) is the
            // exact cross-tenant probing signal security ops needs to correlate. Full detail stays server-side;
            // the UI sees only a coarse, non-disambiguating reason.
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

        // SSRF guard: validate the base URL resolves to a public address before sending the Authorization header.
        // Without this, a workspace admin could point baseUrl at 169.254.169.254 or other internal services and
        // exfiltrate the api key via Authorization: Bearer.
        try {
            AiObservabilityUrlValidator.validateExternalUrl(probeUrl);
        } catch (IllegalArgumentException illegalArgumentException) {
            // WARN-level so a flood of SSRF rejections correlates with audit dashboards: an admin probing
            // private addresses is exactly the cross-tenant signal security ops need to see, not silently
            // absorbed into a UI failure response.
            log.warn(
                "Provider connection test rejected by SSRF guard for workspaceId={} providerId={}",
                workspaceId, providerId, illegalArgumentException);

            // The UI receives a coarse reason; the validator's own message names the reject reason and
            // exposes the structure of the internal blocklist — useful for debugging during onboarding,
            // also useful for an attacker mapping the internal address space. Full detail stays in the
            // warn log above.
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

            // INFO: a JVM shutdown or task cancellation while a probe is in flight is not an outage but should
            // still be correlatable with backend incidents.
            log.info(
                "Provider connection test interrupted for workspaceId={} providerId={}",
                workspaceId, providerId, interruptedException);

            return ProviderConnectionResult.failure("Interrupted while contacting provider");
        } catch (Exception exception) {
            // WARN so backend incidents surface in dashboards even when the UI just shows a coarse "Failed to
            // reach provider" message. SSRF probes, TLS handshake failures, connection-pool exhaustion, and
            // bugs in revealApiKey() must leave a log trail correlatable with a flood of failed UI tests.
            log.warn(
                "Provider connection test failed for workspaceId={} providerId={}",
                workspaceId, providerId, exception);

            // The UI receives a coarse, generic reason — the raw exception message often contains internal
            // hostnames, IPv6 addresses, certificate-chain details, and JDK-version-specific text that an
            // unprivileged workspace admin should not see (and an attacker could enumerate to map internal
            // network shape past the SSRF guard above). Map the most useful concrete categories to short
            // strings; everything else falls back to the same generic reason. Full detail stays server-side
            // in the warn log.
            return ProviderConnectionResult.failure(coarseFailureReason(exception));
        }
    }

    /**
     * Package-private for direct unit tests of the error-mapping taxonomy. Callers outside this class must go through
     * {@code testWorkspaceProviderConnection}, which catches the underlying exceptions and routes through this helper.
     */
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

        AiObservabilityUrlValidator.validateExternalUrl(baseUrl);
    }
}
