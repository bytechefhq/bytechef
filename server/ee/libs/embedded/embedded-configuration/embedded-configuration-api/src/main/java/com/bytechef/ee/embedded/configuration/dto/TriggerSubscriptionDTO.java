/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionErrorCode;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TriggerSubscriptionDTO(
    long id, String componentName, int componentVersion, String triggerName, @Nullable String name,
    @Nullable String externalId, Map<String, ?> input, @Nullable String webhookUrl, TriggerSubscriptionStatus status,
    @Nullable TriggerSubscriptionErrorCode errorCode, @Nullable String errorMessage, @Nullable String staticWebhookUrl,
    @Nullable Instant lastEventDate, @Nullable String lastDeliveryStatus, @Nullable String lastDeliveryError,
    Instant createdDate, Instant lastModifiedDate) {

    public TriggerSubscriptionDTO(TriggerSubscription triggerSubscription, @Nullable String staticWebhookUrl) {
        this(
            Objects.requireNonNull(triggerSubscription.getId()), triggerSubscription.getComponentName(),
            triggerSubscription.getComponentVersion(), triggerSubscription.getTriggerName(),
            triggerSubscription.getName(), triggerSubscription.getExternalId(), triggerSubscription.getParameters(),
            triggerSubscription.getWebhookUrl(), triggerSubscription.getStatus(), triggerSubscription.getErrorCode(),
            triggerSubscription.getErrorMessage(), staticWebhookUrl, triggerSubscription.getLastEventDate(),
            triggerSubscription.getLastDeliveryStatus(), triggerSubscription.getLastDeliveryError(),
            triggerSubscription.getCreatedDate(), triggerSubscription.getLastModifiedDate());
    }
}
