/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class TriggerSubscriptionWebhookSettingsDTOTest {

    @Test
    void testToStringRedactsTheSigningSecret() {
        TriggerSubscriptionWebhookSettingsDTO triggerSubscriptionWebhookSettingsDTO =
            new TriggerSubscriptionWebhookSettingsDTO("https://example.com/hooks", "whsec_secret-value");

        assertThat(triggerSubscriptionWebhookSettingsDTO.toString())
            .contains("https://example.com/hooks")
            .contains("[redacted]")
            .doesNotContain("whsec_secret-value");
    }

    @Test
    void testToStringShowsAMissingSigningSecret() {
        TriggerSubscriptionWebhookSettingsDTO triggerSubscriptionWebhookSettingsDTO =
            new TriggerSubscriptionWebhookSettingsDTO(null, null);

        assertThat(triggerSubscriptionWebhookSettingsDTO.toString())
            .isEqualTo("TriggerSubscriptionWebhookSettingsDTO[webhookUrl=null, signingSecret=null]");
    }
}
