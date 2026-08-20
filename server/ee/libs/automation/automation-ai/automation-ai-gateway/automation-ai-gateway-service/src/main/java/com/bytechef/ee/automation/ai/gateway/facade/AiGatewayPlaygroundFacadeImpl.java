/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Implementation of {@link AiGatewayPlaygroundFacade}. Delegates to the shared {@code AiGatewayFacade} (so playground
 * runs through the full routing/cost-tracking/tracing pipeline) and carries the {@code ADMIN} guard so it is enforced
 * for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayPlaygroundFacadeImpl implements AiGatewayPlaygroundFacade {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayPlaygroundFacadeImpl.class);

    /**
     * Minimum workspace role a playground stream may be billed and traced against. This is a role-rank check, not a
     * scope token: ROLE_ADMIN alone is not enough, because a tenant admin who is not a member of the target workspace
     * could otherwise bill that workspace's budget and pollute its traces by stamping its id on the run.
     */
    private static final String MINIMUM_EDITOR_ROLE = "EDITOR";

    private final AiGatewayFacade aiGatewayFacade;

    @Nullable
    private final PermissionService permissionService;

    /**
     * {@code PermissionService} is optional for the same reason it is on {@code AiGatewayFacadeImpl}: this module is
     * also assembled into {@code ai-gateway-app}, which does not carry {@code automation-configuration-service} and so
     * has no such bean. The streaming entry point that needs the guard is coordinator-only and absent from that app;
     * were it ever reached without the service, {@link #playgroundChatCompletionStream} refuses the run rather than
     * skipping the check.
     */
    @SuppressFBWarnings("EI")
    AiGatewayPlaygroundFacadeImpl(
        AiGatewayFacade aiGatewayFacade, @Nullable PermissionService permissionService) {

        this.aiGatewayFacade = aiGatewayFacade;
        this.permissionService = permissionService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayChatCompletionResponse playgroundChatCompletion(AiGatewayChatCompletionRequest request) {
        return aiGatewayFacade.chatCompletion(request, null);
    }

    /**
     * Carries the workspace-membership guard that used to sit in
     * {@code AiGatewayPlaygroundRestController.chatCompletionsStream}'s body.
     *
     * <p>
     * The guard runs before the returned Flux is built, so a denial surfaces as a synchronous throw on the request
     * &mdash; the SSE entry point turns that into a 403 rather than an {@code event: error} frame on an accepted
     * stream. Request construction stays deferred inside the Flux for the opposite reason: the entry point does want a
     * malformed request framed as an error event.
     *
     * <p>
     * The denial message is uniform ("Not authorized for the requested workspace") so cross-tenant probers cannot
     * enumerate workspace ids by reading the error body &mdash; the diagnostic context is logged server-side here
     * instead.
     */
    @Override
    public Flux<AiGatewayChatCompletionResponse> playgroundChatCompletionStream(
        long workspaceId, Supplier<AiGatewayChatCompletionRequest> requestSupplier,
        @Nullable AtomicLong traceIdHolder) {

        if (permissionService == null) {
            throw new AccessDeniedException("Not authorized for the requested workspace");
        }

        if (!permissionService.hasWorkspaceRole(workspaceId, MINIMUM_EDITOR_ROLE)) {
            log.warn(
                "Playground stream authorization rejected: workspaceId={} requiredRole={}", workspaceId,
                MINIMUM_EDITOR_ROLE);

            throw new AccessDeniedException("Not authorized for the requested workspace");
        }

        return Flux.defer(() -> aiGatewayFacade.chatCompletionStream(requestSupplier.get(), null, null, traceIdHolder));
    }
}
