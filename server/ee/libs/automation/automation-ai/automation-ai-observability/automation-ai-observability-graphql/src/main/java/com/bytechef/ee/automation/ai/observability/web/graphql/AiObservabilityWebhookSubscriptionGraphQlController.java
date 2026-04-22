/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.observability.facade.AiObservabilityWebhookSubscriptionFacade;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityWebhookDeliveryService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityWebhookSubscriptionService;
import com.bytechef.ee.automation.ai.observability.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookDelivery;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityWebhookDeliveryService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityWebhookSubscriptionService;
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
    private final AiObservabilityWebhookSubscriptionFacade aiObservabilityWebhookSubscriptionFacade;
    private final WorkspaceAiObservabilityWebhookDeliveryService workspaceAiObservabilityWebhookDeliveryService;
    private final WorkspaceAiObservabilityWebhookSubscriptionService workspaceAiObservabilityWebhookSubscriptionService;
    private final AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilityWebhookSubscriptionGraphQlController(
        AiObservabilityWebhookDeliveryService aiObservabilityWebhookDeliveryService,
        AiObservabilityWebhookSubscriptionFacade aiObservabilityWebhookSubscriptionFacade,
        WorkspaceAiObservabilityWebhookDeliveryService workspaceAiObservabilityWebhookDeliveryService,
        WorkspaceAiObservabilityWebhookSubscriptionService workspaceAiObservabilityWebhookSubscriptionService,
        AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilityWebhookDeliveryService = aiObservabilityWebhookDeliveryService;
        this.aiObservabilityWebhookSubscriptionFacade = aiObservabilityWebhookSubscriptionFacade;
        this.workspaceAiObservabilityWebhookDeliveryService = workspaceAiObservabilityWebhookDeliveryService;
        this.workspaceAiObservabilityWebhookSubscriptionService = workspaceAiObservabilityWebhookSubscriptionService;
        this.aiObservabilityWebhookSubscriptionService = aiObservabilityWebhookSubscriptionService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityWebhookSubscription aiObservabilityWebhookSubscription(@Argument long id) {
        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityWebhookSubscriptionService.getWorkspaceId(subscription.getId()), "VIEWER");

        return subscription;
    }

    @QueryMapping
    public List<AiObservabilityWebhookSubscription> aiObservabilityWebhookSubscriptions(
        @Argument Long workspaceId) {

        // Authorization (workspace VIEWER role) is enforced on AiObservabilityWebhookSubscriptionFacade so it protects
        // every caller of the facade, not just this GraphQL entry point.
        return aiObservabilityWebhookSubscriptionFacade.getWebhookSubscriptionsByWorkspace(workspaceId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<AiObservabilityWebhookDelivery> aiObservabilityWebhookDeliveries(@Argument long subscriptionId) {
        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(subscriptionId);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityWebhookSubscriptionService.getWorkspaceId(subscription.getId()), "VIEWER");

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
            new AiObservabilityWebhookSubscription(name, url, events);

        subscription.setProjectId(projectId);
        subscription.setSecret(secret);
        subscription.setEnabled(enabled);

        return workspaceAiObservabilityWebhookSubscriptionService.createInWorkspace(subscription, workspaceId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityWebhookSubscription updateAiObservabilityWebhookSubscription(
        @Argument long id, @Argument String name, @Argument String url,
        @Argument String secret, @Argument String events, @Argument boolean enabled) {

        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityWebhookSubscriptionService.getWorkspaceId(subscription.getId()), "EDITOR");

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

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityWebhookSubscriptionService.getWorkspaceId(subscription.getId()), "EDITOR");

        aiObservabilityWebhookSubscriptionService.delete(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean testAiObservabilityWebhookSubscription(@Argument long id) {
        AiObservabilityWebhookSubscription subscription =
            aiObservabilityWebhookSubscriptionService.getWebhookSubscription(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilityWebhookSubscriptionService.getWorkspaceId(subscription.getId()), "EDITOR");

        workspaceAiObservabilityWebhookDeliveryService.deliverTestEvent(id);

        return true;
    }

}
