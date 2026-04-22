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
public enum AiGatewayContentBlockType {

    DOCUMENT("document"),
    IMAGE("image"),
    IMAGE_URL("image_url"),
    TEXT("text");

    private final String value;

    AiGatewayContentBlockType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AiGatewayContentBlockType fromValue(String value) {
        for (AiGatewayContentBlockType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown content block type: " + value);
    }
}
