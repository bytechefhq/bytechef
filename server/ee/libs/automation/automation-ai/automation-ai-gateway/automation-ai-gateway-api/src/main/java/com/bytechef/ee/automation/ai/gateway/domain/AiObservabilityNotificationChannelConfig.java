/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import com.bytechef.commons.util.JsonUtils;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * Typed per-type configuration for {@link AiObservabilityNotificationChannel}. Replaces the previous opaque
 * {@code config} JSON blob with a sealed hierarchy: each channel type has its own required fields, validated at
 * construction time instead of re-checked ad-hoc in every dispatcher branch.
 *
 * <p>
 * Storage: the {@code config} column remains a {@code String} holding the JSON for this type. Consumers call
 * {@link #fromJson(String)}/{@link #toJson(AiObservabilityNotificationChannelConfig)} to round-trip; the Jackson
 * discriminator ({@code "type"} property) preserves the variant.
 *
 * @version ee
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AiObservabilityNotificationChannelConfig.Webhook.class, name = "WEBHOOK"),
    @JsonSubTypes.Type(value = AiObservabilityNotificationChannelConfig.Email.class, name = "EMAIL"),
    @JsonSubTypes.Type(value = AiObservabilityNotificationChannelConfig.Slack.class, name = "SLACK")
})
public sealed interface AiObservabilityNotificationChannelConfig {

    AiObservabilityNotificationChannelType channelType();

    /**
     * Webhook channel config. URL is validated by the dispatcher at send time via {@code AiGatewayUrlValidator} (SSRF
     * guard); an optional shared HMAC secret may be sent in {@code X-ByteChef-Signature} headers.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    record Webhook(String url, String secret, Map<String, String> headers)
        implements AiObservabilityNotificationChannelConfig {

        public Webhook {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("Webhook.url must not be blank");
            }

            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }

        @Override
        public AiObservabilityNotificationChannelType channelType() {
            return AiObservabilityNotificationChannelType.WEBHOOK;
        }
    }

    /**
     * Email channel config. {@code recipients} must be non-empty because a channel that sends to nobody silently no-ops
     * — a class of misconfiguration that produced support tickets for other notification systems in the codebase.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    record Email(List<String> recipients, String subject)
        implements AiObservabilityNotificationChannelConfig {

        public Email {
            if (recipients == null || recipients.isEmpty()) {
                throw new IllegalArgumentException("Email.recipients must be non-empty");
            }

            recipients = List.copyOf(recipients);
        }

        @Override
        public AiObservabilityNotificationChannelType channelType() {
            return AiObservabilityNotificationChannelType.EMAIL;
        }
    }

    /**
     * Slack channel config. Either a bot token + channel, or an incoming-webhook URL — exactly one must be populated.
     * Construction rejects both-null and both-non-null inputs so the dispatcher does not need to re-check at send time.
     */
    record Slack(String webhookUrl, String botToken, String channel)
        implements AiObservabilityNotificationChannelConfig {

        public Slack {
            boolean hasWebhook = webhookUrl != null && !webhookUrl.isBlank();
            boolean hasBot = botToken != null && !botToken.isBlank()
                && channel != null && !channel.isBlank();

            if (hasWebhook == hasBot) {
                throw new IllegalArgumentException(
                    "Slack config must specify exactly one of (webhookUrl) or (botToken + channel)");
            }
        }

        @Override
        public AiObservabilityNotificationChannelType channelType() {
            return AiObservabilityNotificationChannelType.SLACK;
        }
    }

    static AiObservabilityNotificationChannelConfig fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        return JsonUtils.read(json, AiObservabilityNotificationChannelConfig.class);
    }

    static String toJson(AiObservabilityNotificationChannelConfig config) {
        return config == null ? null : JsonUtils.write(config);
    }
}
