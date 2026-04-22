/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @version ee
 */
public enum AiGatewayChatRole {

    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool"),
    USER("user");

    private final String value;

    AiGatewayChatRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AiGatewayChatRole fromValue(String value) {
        for (AiGatewayChatRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown chat role: " + value);
    }
}
