/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record TriggerSubscriptionWebhookSettingsDTO(@Nullable String webhookUrl, @Nullable String signingSecret) {

    @Override
    public String toString() {
        return "TriggerSubscriptionWebhookSettingsDTO[webhookUrl=" + webhookUrl + ", signingSecret=" +
            (signingSecret == null ? null : "[redacted]") + "]";
    }
}
