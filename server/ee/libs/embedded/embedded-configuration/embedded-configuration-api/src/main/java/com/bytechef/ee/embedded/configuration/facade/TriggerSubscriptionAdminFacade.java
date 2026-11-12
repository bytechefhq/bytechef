/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionDisableReason;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionTestEventResultDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionWebhookSettingsDTO;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface TriggerSubscriptionAdminFacade {

    void deleteConnectedUserTriggerSubscriptions(long connectedUserId);

    void deleteTriggerSubscription(long id);

    void disableConnectedUserTriggerSubscriptions(long connectedUserId, TriggerSubscriptionDisableReason reason);

    void enableTriggerSubscription(long id, boolean enable);

    List<TriggerSubscriptionDTO> getConnectedUserTriggerSubscriptions(long connectedUserId);

    TriggerSubscriptionWebhookSettingsDTO getWebhookSettings(long environmentId);

    TriggerSubscriptionWebhookSettingsDTO rotateSigningSecret(long environmentId);

    TriggerSubscriptionTestEventResultDTO sendTestEvent(long environmentId);

    TriggerSubscriptionWebhookSettingsDTO updateWebhookUrl(long environmentId, @Nullable String webhookUrl);
}
