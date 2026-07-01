/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.agent.tool;

import com.bytechef.commons.util.JsonUtils;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ToolErrors {

    private static final Logger log = LoggerFactory.getLogger(ToolErrors.class);

    private ToolErrors() {
    }

    /**
     * Serialises a {@code {"error": <message>}} JSON object using the supplied {@link JsonMapper}.
     *
     * @param message the human-readable error message
     * @return a JSON string of the form {@code {"error":"<message>"}} or the fallback {@code {"error":"serialization
     *         failure"}}
     */
    public static String toolError(String message) {
        try {
            return JsonUtils.write(Map.of("error", message));
        } catch (JacksonException exception) {
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
}
