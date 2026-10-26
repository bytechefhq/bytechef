/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import org.apache.commons.lang3.Validate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AiGatewayContentBlock(
    AiGatewayContentBlockType type, String text, ImageUrl imageUrl, Document document) {
    public AiGatewayContentBlock {
        Validate.notNull(type, "type must not be null");

        switch (type) {
            case TEXT -> Validate.notBlank(text, "text required for TEXT block");
            case IMAGE_URL, IMAGE -> Validate.notNull(imageUrl, "imageUrl required for IMAGE_URL/IMAGE block");
            case DOCUMENT -> Validate.notNull(document, "document required for DOCUMENT block");
            default -> throw new IllegalArgumentException("Unsupported content block type: " + type);
        }
    }

    public static AiGatewayContentBlock ofText(String text) {
        return new AiGatewayContentBlock(AiGatewayContentBlockType.TEXT, text, null, null);
    }

    public static AiGatewayContentBlock ofImageUrl(String url) {
        return new AiGatewayContentBlock(
            AiGatewayContentBlockType.IMAGE_URL, null, new ImageUrl(url, null), null);
    }

    public record ImageUrl(String url, String detail) {
    }

    public record Document(String mediaType, String sourceType, String data, String url) {
    }
}
