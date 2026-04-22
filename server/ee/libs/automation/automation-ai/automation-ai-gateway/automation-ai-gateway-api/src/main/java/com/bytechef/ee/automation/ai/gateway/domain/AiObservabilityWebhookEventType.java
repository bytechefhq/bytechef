/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import com.bytechef.commons.util.JsonUtils;
import java.util.List;

/**
 * The closed set of webhook event discriminators. {@link AiObservabilityWebhookSubscription#getEvents()} was a
 * free-form JSON array of strings; this enum pins the names so a typo in a subscription row — previously a silent
 * "never fires" — now fails at parse time. Dispatchers (e.g. {@code AiObservabilityWebhookDeliveryServiceImpl}) compare
 * their outgoing event name against this enum and reject unknown values.
 *
 * <p>
 * Adoption: existing rows store event names as strings; {@link #parseList(String)} handles both legacy and typed forms.
 * New rows should go through {@link #toJson(List)} to ensure every entry is a canonical enum name.
 *
 * <p>
 * <b>Storage strategy:</b> event names are stored as a JSON array of stable wire strings (e.g.
 * {@code ["trace.completed","budget.exceeded"]}), NOT as INT ordinals. This deliberately diverges from the CLAUDE.md
 * "JDBC enums as INT ordinals" convention used by {@link AiGatewayProviderType} and similar scalar enum columns.
 * Reasons:
 * <ol>
 * <li>A single subscription can be subscribed to a subset of events — a scalar ordinal column cannot express a set
 * without a junction table, and a comma-separated ordinal string is harder to hand-inspect in SQL than wire names.</li>
 * <li>Wire names are also the over-the-wire payload discriminator delivered to the subscriber's webhook URL; storing
 * them as strings keeps the DB row and the outbound HTTP body in the same vocabulary.</li>
 * <li>Decoupling enum constant name (e.g. {@code TRACE_COMPLETED}) from wire name ({@code trace.completed}) lets us
 * rename Java constants safely — with ordinals, a rename is silent but a reorder is silently breaking.</li>
 * </ol>
 * If a new value is added, append it at the end and leave legacy rows untouched — {@link #fromWire(String)} ignores
 * ordering.
 *
 * @version ee
 */
public enum AiObservabilityWebhookEventType {
    TRACE_COMPLETED("trace.completed"),
    BUDGET_EXCEEDED("budget.exceeded"),
    ALERT_TRIGGERED("alert.triggered"),
    ALERT_RESOLVED("alert.resolved"),
    EXPORT_READY("export.ready");

    private final String wireName;

    AiObservabilityWebhookEventType(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static AiObservabilityWebhookEventType fromWire(String wire) {
        if (wire == null) {
            return null;
        }

        for (AiObservabilityWebhookEventType type : values()) {
            if (type.wireName.equals(wire)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown webhook event type: " + wire);
    }

    /**
     * Parses a JSON array of wire names (e.g. {@code ["trace.completed","budget.exceeded"]}) into typed events. Unknown
     * names bubble up — a silent "subscription that never fires" was the pre-fix failure mode.
     */
    public static List<AiObservabilityWebhookEventType> parseList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        List<String> wires = JsonUtils.readList(json, String.class);

        return wires.stream()
            .map(AiObservabilityWebhookEventType::fromWire)
            .toList();
    }

    public static String toJson(List<AiObservabilityWebhookEventType> events) {
        if (events == null || events.isEmpty()) {
            return "[]";
        }

        List<String> wires = events.stream()
            .map(AiObservabilityWebhookEventType::wireName)
            .toList();

        return JsonUtils.write(wires);
    }
}
