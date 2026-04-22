/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookDelivery;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityWebhookDeliveryService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityWebhookSubscriptionService;
import com.bytechef.ee.automation.ai.gateway.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilityWebhookSubscriptionGraphQlController {

    private final AiObservabilityWebhookDeliveryService aiObservabilityWebhookDeliveryService;
    private final AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilityWebhookSubscriptionGraphQlController(
        AiObservabilityWebhookDeliveryService aiObservabilityWebhookDeliveryService,
        AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilityWebhookDeliveryService = aiObservabilityWebhookDeliveryService;
        this.aiObservabilityWebhookSubscriptionService = aiObservabilityWebhookSubscriptionService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityWebhookSubscription aiObservabilityWebhookSubscription(@Argument long id) {
        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(id);

        workspaceAuthorization.requireWorkspaceRole(subscription.getWorkspaceId(), "VIEWER");

        return subscription;
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<AiObservabilityWebhookSubscription> aiObservabilityWebhookSubscriptions(
        @Argument Long workspaceId) {

        return aiObservabilityWebhookSubscriptionService.getWebhookSubscriptionsByWorkspace(workspaceId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<AiObservabilityWebhookDelivery> aiObservabilityWebhookDeliveries(@Argument long subscriptionId) {
        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(subscriptionId);

        workspaceAuthorization.requireWorkspaceRole(subscription.getWorkspaceId(), "VIEWER");

        return aiObservabilityWebhookDeliveryService.getDeliveriesBySubscription(subscriptionId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityWebhookSubscription createAiObservabilityWebhookSubscription(
        @Argument Long workspaceId, @Argument Long projectId, @Argument String name,
        @Argument String url, @Argument String secret, @Argument String events,
        @Argument boolean enabled) {

        workspaceAuthorization.requireWorkspaceRole(workspaceId, "EDITOR");

        AiObservabilityWebhookSubscription subscription =
            new AiObservabilityWebhookSubscription(workspaceId, name, url, events);

        subscription.setProjectId(projectId);
        subscription.setSecret(secret);
        subscription.setEnabled(enabled);

        return aiObservabilityWebhookSubscriptionService.create(subscription);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityWebhookSubscription updateAiObservabilityWebhookSubscription(
        @Argument long id, @Argument String name, @Argument String url,
        @Argument String secret, @Argument String events, @Argument boolean enabled) {

        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(id);

        workspaceAuthorization.requireWorkspaceRole(subscription.getWorkspaceId(), "EDITOR");

        subscription.setName(name);
        subscription.setUrl(url);

        // Only overwrite the HMAC secret when the client explicitly sends a new value. The existing secret is
        // encrypted and @JsonIgnored on the getter, so the UI cannot round-trip it; an unconditional setSecret(secret)
        // would wipe the signing key on every edit (rename, enable/disable toggle) and silently break signature
        // verification on the receiver.
        if (secret != null) {
            subscription.setSecret(secret);
        }

        subscription.setEvents(events);
        subscription.setEnabled(enabled);

        return aiObservabilityWebhookSubscriptionService.update(subscription);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteAiObservabilityWebhookSubscription(@Argument long id) {
        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(id);

        workspaceAuthorization.requireWorkspaceRole(subscription.getWorkspaceId(), "EDITOR");

        aiObservabilityWebhookSubscriptionService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean testAiObservabilityWebhookSubscription(@Argument long id) {
        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(id);

        workspaceAuthorization.requireWorkspaceRole(subscription.getWorkspaceId(), "EDITOR");

        aiObservabilityWebhookDeliveryService.deliverTestEvent(id);

        return true;
    }

}
