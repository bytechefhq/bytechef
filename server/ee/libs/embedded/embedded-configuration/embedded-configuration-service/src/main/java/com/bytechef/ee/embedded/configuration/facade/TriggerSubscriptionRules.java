/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.commons.util.UrlValidatorUtils;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionErrorType;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Rules shared by the public and the admin trigger subscription facades.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class TriggerSubscriptionRules {

    private TriggerSubscriptionRules() {
    }

    static void checkConnectedUserEnabled(ConnectedUser connectedUser) {
        if (!connectedUser.isEnabled()) {
            throw new ConfigurationException(
                "Connected user %d is disabled".formatted(connectedUser.getId()),
                TriggerSubscriptionErrorType.USER_INPUT_ERROR);
        }
    }

    static void checkEnabled(IntegrationInstance integrationInstance) {
        if (!integrationInstance.isEnabled()) {
            throw new ConfigurationException(
                "Integration instance %d is disabled".formatted(integrationInstance.getId()),
                TriggerSubscriptionErrorType.USER_INPUT_ERROR);
        }
    }

    static void checkIntegrationInstanceEnabled(
        TriggerSubscription triggerSubscription, IntegrationInstanceService integrationInstanceService) {

        Long integrationInstanceId = triggerSubscription.getIntegrationInstanceId();

        if (integrationInstanceId == null) {
            return;
        }

        Optional<IntegrationInstance> integrationInstanceOptional =
            integrationInstanceService.fetchIntegrationInstance(integrationInstanceId);

        if (integrationInstanceOptional.isEmpty()) {
            throw new ConfigurationException(
                "Integration instance %d no longer exists".formatted(integrationInstanceId),
                TriggerSubscriptionErrorType.USER_INPUT_ERROR);
        }

        checkEnabled(integrationInstanceOptional.get());
    }

    static void checkTriggerSubscriptionLimit(
        long connectedUserId, int triggerSubscriptionCount, int maxTriggerSubscriptions) {

        if (maxTriggerSubscriptions > 0 && triggerSubscriptionCount >= maxTriggerSubscriptions) {
            throw new ConfigurationException(
                "Connected user %d already has the maximum of %d trigger subscriptions".formatted(
                    connectedUserId, maxTriggerSubscriptions),
                TriggerSubscriptionErrorType.USER_INPUT_ERROR);
        }
    }

    static void checkTriggerExists(
        TriggerDefinitionService triggerDefinitionService, String componentName, int componentVersion,
        String triggerName) {

        try {
            triggerDefinitionService.getTriggerDefinition(componentName, componentVersion, triggerName);
        } catch (IllegalArgumentException exception) {
            throw new ConfigurationException(
                "Trigger %s/v%d/%s does not exist".formatted(componentName, componentVersion, triggerName),
                exception, TriggerSubscriptionErrorType.NO_TRIGGER);
        }
    }

    static void validateWebhookUrl(@Nullable String webhookUrl, Set<String> allowedWebhookHosts) {
        if (webhookUrl == null) {
            return;
        }

        try {
            UrlValidatorUtils.validate(webhookUrl, allowedWebhookHosts);
        } catch (UrlValidationException urlValidationException) {
            throw new ConfigurationException(
                "Webhook URL is not allowed: " + urlValidationException.getMessage(), urlValidationException,
                TriggerSubscriptionErrorType.USER_INPUT_ERROR);
        }
    }
}
