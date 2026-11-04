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

package com.bytechef.ai.agent.tool;

import com.bytechef.commons.util.JsonUtils;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public final class ToolErrors {

    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");

    private static final Logger log = LoggerFactory.getLogger(ToolErrors.class);

    private ToolErrors() {
    }

    /**
     * Serialises a {@code {"error": <message>}} JSON object using {@link JsonUtils}.
     *
     * @param message the human-readable error message
     * @return a JSON string of the form {@code {"error":"<message>"}} or the fallback {@code {"error":"serialization
     *         failure"}}
     */
    public static String toolError(String message) {
        try {
            return JsonUtils.write(Map.of("error", message));
        } catch (RuntimeException exception) {
            log.error(
                "Failed to serialise tool error response for message '{}': {}", message, exception.toString(),
                exception);

            return "{\"error\":\"serialization failure\"}";
        }
    }

    /**
     * Logs a tool-callback {@link RuntimeException} at WARN with sanitised exception text and returns a typed tool
     * error string the agent loop can recover from. Use as the {@code catch (RuntimeException)} arm of every
     * {@code call(toolInput, toolContext)} method so a transient DB outage, NPE, or 4xx/5xx from a downstream service
     * does not abort the entire agent run.
     *
     * @param sourceClass the calling tool callback's class — used to resolve the SLF4J log so the WARN line is
     *                    attributed to the right component instead of {@code ToolErrors}
     * @param toolName    human-readable tool name surfaced to the LLM in the error payload
     * @param exception   the runtime exception that escaped the callback's main try block
     * @return a JSON string of the form {@code {"error":"<toolName> failed (<simpleName>)"}}
     */
    public static String runtimeFailure(
        Class<?> sourceClass, String toolName, RuntimeException exception) {

        Logger sourceLogger = LoggerFactory.getLogger(sourceClass);

        sourceLogger.warn("{} failed: {}", toolName, exception.toString(), exception);

        Class<? extends RuntimeException> exceptionClass = exception.getClass();

        return toolError(toolName + " failed (" + exceptionClass.getSimpleName() + ")");
    }

    public static String toolError(JsonMapper jsonMapper, String message) {
        try {
            return jsonMapper.writeValueAsString(Map.of("error", message));
        } catch (RuntimeException exception) {
            log.error(
                "Failed to serialise tool error response for message '{}': {}", message, exception.toString(),
                exception);

            return "{\"error\":\"serialization failure\"}";
        }
    }

    public static String runtimeFailure(
        JsonMapper jsonMapper, Class<?> sourceClass, String toolName, RuntimeException exception) {

        Logger sourceLogger = LoggerFactory.getLogger(sourceClass);

        sourceLogger.warn("{} failed: {}", toolName, exception.toString(), exception);

        Class<? extends RuntimeException> exceptionClass = exception.getClass();

        return toolError(jsonMapper, toolName + " failed (" + exceptionClass.getSimpleName() + ")");
    }

    public static String invalidInput(JacksonException exception) {
        return toolError(describeInvalidInput(exception));
    }

    private static String describeInvalidInput(JacksonException exception) {
        if (exception instanceof StreamReadException) {
            return "Invalid input: not valid JSON";
        }

        String fieldPath = toSafeFieldPath(exception.getPath());

        if (fieldPath == null) {
            return "Invalid input: does not match the input schema";
        }

        return "Invalid input: unexpected value for '" + fieldPath + "'";
    }

    @Nullable
    private static String toSafeFieldPath(List<JacksonException.Reference> references) {
        StringBuilder fieldPath = new StringBuilder();

        for (JacksonException.Reference reference : references) {
            String propertyName = reference.getPropertyName();

            if (propertyName != null) {
                Matcher matcher = FIELD_NAME_PATTERN.matcher(propertyName);

                if (!matcher.matches()) {
                    return null;
                }

                if (!fieldPath.isEmpty()) {
                    fieldPath.append('.');
                }

                fieldPath.append(propertyName);
            } else if (reference.getIndex() >= 0) {
                fieldPath.append('[')
                    .append(reference.getIndex())
                    .append(']');
            } else {
                return null;
            }
        }

        if (fieldPath.isEmpty()) {
            return null;
        }

        return fieldPath.toString();
    }
}
