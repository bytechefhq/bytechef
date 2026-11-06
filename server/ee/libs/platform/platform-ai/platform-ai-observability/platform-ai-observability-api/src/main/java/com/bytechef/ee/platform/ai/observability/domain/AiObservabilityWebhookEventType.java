/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.domain;

import com.bytechef.commons.util.JsonUtils;
import java.util.List;

/**
 * @author Ivica Cardic
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
