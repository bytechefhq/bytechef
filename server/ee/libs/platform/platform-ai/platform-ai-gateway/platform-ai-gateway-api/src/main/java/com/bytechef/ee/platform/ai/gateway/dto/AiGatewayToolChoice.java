/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.Validate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AiGatewayToolChoice(String stringValue, ToolRef toolRef) {

    public AiGatewayToolChoice {
        Validate.isTrue((stringValue != null) ^ (toolRef != null), "exactly one of stringValue or toolRef must be set");
    }

    public static AiGatewayToolChoice ofString(String value) {
        return new AiGatewayToolChoice(value, null);
    }

    public static AiGatewayToolChoice ofTool(String toolName) {
        return new AiGatewayToolChoice(null, new ToolRef("function", toolName));
    }

    @JsonIgnore
    public boolean isStringValue() {
        return stringValue != null;
    }

    public Object toOpenAiValue() {
        if (stringValue != null) {
            return stringValue;
        }

        if (toolRef != null) {
            return toolRef;
        }

        throw new IllegalStateException("AiGatewayToolChoice has neither stringValue nor toolRef");
    }

    public record ToolRef(String type, String name) {
    }
}
