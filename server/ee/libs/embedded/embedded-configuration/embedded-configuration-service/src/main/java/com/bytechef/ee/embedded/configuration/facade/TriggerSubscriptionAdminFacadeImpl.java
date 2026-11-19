/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.audit.TriggerSubscriptionAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionDisableReason;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionTestEventResultDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionWebhookSettingsDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.ee.embedded.configuration.trigger.subscription.TriggerSubscriptionEventSender;
import com.bytechef.ee.embedded.configuration.trigger.subscription.TriggerSubscriptionWebhookSettingsAccessor;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookTestResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class TriggerSubscriptionAdminFacadeImpl implements TriggerSubscriptionAdminFacade {

    private static final String NO_SIGNING_SECRET_MESSAGE = "No signing secret is configured for this environment";
    private static final String NO_WEBHOOK_URL_MESSAGE = "No webhook URL is configured for this environment";

    private final Set<String> allowedWebhookHosts;
    private final ConnectedUserService connectedUserService;
    private final EnvironmentService environmentService;
    private final IntegrationInstanceService integrationInstanceService;
    private final TriggerSubscriptionEventSender triggerSubscriptionEventSender;
    private final TriggerSubscriptionManager triggerSubscriptionManager;
    private final TriggerSubscriptionService triggerSubscriptionService;
    private final TriggerSubscriptionWebhookSettingsAccessor triggerSubscriptionWebhookSettingsAccessor;
    private final WebhookDeliveryService webhookDeliveryService;

    @SuppressFBWarnings("EI")
    public TriggerSubscriptionAdminFacadeImpl(
        ApplicationProperties applicationProperties, ConnectedUserService connectedUserService,
        EnvironmentService environmentService, IntegrationInstanceService integrationInstanceService,
        TriggerSubscriptionEventSender triggerSubscriptionEventSender,
        TriggerSubscriptionManager triggerSubscriptionManager, TriggerSubscriptionService triggerSubscriptionService,
        TriggerSubscriptionWebhookSettingsAccessor triggerSubscriptionWebhookSettingsAccessor,
        WebhookDeliveryService webhookDeliveryService) {

        ApplicationProperties.WebhookDelivery webhookDelivery = applicationProperties.getWebhookDelivery();

        this.allowedWebhookHosts = Set.copyOf(webhookDelivery.getAllowedHosts());
        this.connectedUserService = connectedUserService;
        this.environmentService = environmentService;
        this.integrationInstanceService = integrationInstanceService;
        this.triggerSubscriptionEventSender = triggerSubscriptionEventSender;
        this.triggerSubscriptionManager = triggerSubscriptionManager;
        this.triggerSubscriptionService = triggerSubscriptionService;
        this.triggerSubscriptionWebhookSettingsAccessor = triggerSubscriptionWebhookSettingsAccessor;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteConnectedUserTriggerSubscriptions(long connectedUserId) {
        triggerSubscriptionManager.deleteByConnectedUserId(connectedUserId);
    }

    @Override
    @Audited(event = TriggerSubscriptionAuditEvents.TRIGGER_SUBSCRIPTION_DELETED, data = {
        @AuditData(key = "triggerSubscriptionId", value = "#id")
    })
    @PreAuthorize("isTenantAdmin()")
    public void deleteTriggerSubscription(long id) {
        triggerSubscriptionManager.delete(id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void disableConnectedUserTriggerSubscriptions(
        long connectedUserId, TriggerSubscriptionDisableReason reason) {

        triggerSubscriptionManager.disableByConnectedUserId(connectedUserId, reason);
    }

    @Override
    @Audited(event = TriggerSubscriptionAuditEvents.TRIGGER_SUBSCRIPTION_ENABLED_UPDATED, data = {
        @AuditData(key = "triggerSubscriptionId", value = "#id"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("isTenantAdmin()")
    public void enableTriggerSubscription(long id, boolean enable) {
        if (enable) {
            TriggerSubscription triggerSubscription = triggerSubscriptionService.getTriggerSubscription(id);

            TriggerSubscriptionRules.checkIntegrationInstanceEnabled(triggerSubscription, integrationInstanceService);
            TriggerSubscriptionRules.checkConnectedUserEnabled(
                connectedUserService.getConnectedUser(triggerSubscription.getConnectedUserId()));

            triggerSubscriptionManager.enable(id);
        } else {
            triggerSubscriptionManager.disable(id, null);
        }
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<TriggerSubscriptionDTO> getConnectedUserTriggerSubscriptions(long connectedUserId) {
        return triggerSubscriptionService.getConnectedUserTriggerSubscriptions(connectedUserId)
            .stream()
            .map(triggerSubscription -> new TriggerSubscriptionDTO(
                triggerSubscription, triggerSubscriptionManager.getStaticWebhookUrl(triggerSubscription)))
            .toList();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public TriggerSubscriptionWebhookSettingsDTO getWebhookSettings(long environmentId) {
        return triggerSubscriptionWebhookSettingsAccessor.getSettings(environmentService.getEnvironment(environmentId));
    }

    @Override
    @Audited(event = TriggerSubscriptionAuditEvents.TRIGGER_SUBSCRIPTION_SIGNING_SECRET_ROTATED, data = {
        @AuditData(key = "environmentId", value = "#environmentId")
    })
    @PreAuthorize("isTenantAdmin()")
    public TriggerSubscriptionWebhookSettingsDTO rotateSigningSecret(long environmentId) {
        return triggerSubscriptionWebhookSettingsAccessor.rotateSigningSecret(
            environmentService.getEnvironment(environmentId));
    }

    @Override
    @Audited(event = TriggerSubscriptionAuditEvents.TRIGGER_SUBSCRIPTION_TEST_EVENT_SENT, data = {
        @AuditData(key = "environmentId", value = "#environmentId"),
        @AuditData(key = "success", value = "#result?.success"),
        @AuditData(key = "statusCode", value = "#result?.statusCode")
    })
    @PreAuthorize("isTenantAdmin()")
    public TriggerSubscriptionTestEventResultDTO sendTestEvent(long environmentId) {
        Environment environment = environmentService.getEnvironment(environmentId);

        TriggerSubscriptionWebhookSettingsDTO settings = triggerSubscriptionWebhookSettingsAccessor.getSettings(
            environment);

        String webhookUrl = settings.webhookUrl();

        if (webhookUrl == null || webhookUrl.isBlank()) {
            return new TriggerSubscriptionTestEventResultDTO(false, null, NO_WEBHOOK_URL_MESSAGE);
        }

        String signingSecret = settings.signingSecret();

        if (signingSecret == null || signingSecret.isBlank()) {
            return new TriggerSubscriptionTestEventResultDTO(false, null, NO_SIGNING_SECRET_MESSAGE);
        }

        WebhookDelivery webhookDelivery = triggerSubscriptionEventSender.createTestDelivery(
            environment, webhookUrl, signingSecret);

        WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(webhookDelivery);

        return new TriggerSubscriptionTestEventResultDTO(
            webhookTestResult.success(), webhookTestResult.statusCode(), webhookTestResult.error());
    }

    @Override
    @Audited(event = TriggerSubscriptionAuditEvents.TRIGGER_SUBSCRIPTION_WEBHOOK_URL_UPDATED, data = {
        @AuditData(key = "environmentId", value = "#environmentId")
    })
    @PreAuthorize("isTenantAdmin()")
    public TriggerSubscriptionWebhookSettingsDTO updateWebhookUrl(long environmentId, @Nullable String webhookUrl) {
        String nonBlankWebhookUrl = StringUtils.isBlank(webhookUrl) ? null : webhookUrl;

        TriggerSubscriptionRules.validateWebhookUrl(nonBlankWebhookUrl, allowedWebhookHosts);

        return triggerSubscriptionWebhookSettingsAccessor.saveWebhookUrl(
            environmentService.getEnvironment(environmentId), nonBlankWebhookUrl);
    }
}
