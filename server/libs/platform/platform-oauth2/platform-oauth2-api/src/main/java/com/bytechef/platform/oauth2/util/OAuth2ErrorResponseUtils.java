/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.oauth2.util;

import com.bytechef.commons.util.JsonUtils;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class OAuth2ErrorResponseUtils {

    static final int MAX_FIELD_LENGTH = 500;
    static final int MAX_LOGGED_BODY_LENGTH = 2000;

    private static final Logger log = LoggerFactory.getLogger(OAuth2ErrorResponseUtils.class);

    private OAuth2ErrorResponseUtils() {
    }

    public static String describe(int statusCode, @Nullable String body) {
        if (log.isDebugEnabled()) {
            log.debug(
                "OAuth2 error response with HTTP {}: {}", statusCode,
                sanitizeForLog(truncate(body, MAX_LOGGED_BODY_LENGTH)));
        }

        Map<?, ?> fields = readFields(body);

        String error = getField(fields, "error");
        String errorDescription = getField(fields, "error_description");

        if (errorDescription == null && fields.get("error") instanceof Map<?, ?> nestedError) {
            errorDescription = getField(nestedError, "message");
        }

        if (errorDescription == null) {
            errorDescription = getField(fields, "message");
        }

        String description = "HTTP " + statusCode;

        if (error != null) {
            description = description + ": " + error;
        }

        if (errorDescription != null) {
            description = error == null
                ? description + ": " + errorDescription : description + " (" + errorDescription + ")";
        }

        return description;
    }

    private static @Nullable String getField(Map<?, ?> fields, String name) {
        if (!(fields.get(name) instanceof String value) || value.isBlank()) {
            return null;
        }

        return truncate(value, MAX_FIELD_LENGTH);
    }

    static @Nullable String sanitizeForLog(@Nullable String value) {
        if (value == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(value.length());

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            if (character == '\r') {
                sb.append("\\r");
            } else if (character == '\n') {
                sb.append("\\n");
            } else if (character == '\t') {
                sb.append("\\t");
            } else if (Character.isISOControl(character) || Character.getType(character) == Character.FORMAT ||
                character == '\u2028' || character == '\u2029') {

                sb.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
            } else {
                sb.append(character);
            }
        }

        return sb.toString();
    }

    private static @Nullable String truncate(@Nullable String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private static Map<?, ?> readFields(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }

        try {
            Object value = JsonUtils.read(body, Object.class);

            return value instanceof Map<?, ?> map ? map : Map.of();
        } catch (RuntimeException runtimeException) {
            return Map.of();
        }
    }
}
