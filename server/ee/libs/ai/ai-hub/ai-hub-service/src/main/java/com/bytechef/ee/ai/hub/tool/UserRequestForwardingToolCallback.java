/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * Appends the user's latest chat message, verbatim, to the request the AI Hub hands a workflow-building specialist, so
 * the specialist builds what the user asked for rather than only the hub's paraphrase of it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class UserRequestForwardingToolCallback implements ToolCallback {

    static final String USER_MESSAGE_HEADER = """


        The user's latest message, verbatim. When it describes what to build, it is authoritative: where the \
        request above names components, parameters, steps or constraints the user did not ask for, follow the user's \
        message and build it with the platform's native components and cluster elements. When it only answers a \
        question or confirms a step, apply that answer within the request above.
        """;

    private static final String REQUEST = "request";

    private final ToolCallback delegate;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public UserRequestForwardingToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String userMessage = fetchUserMessage(toolContext);

        if (userMessage == null) {
            return delegate.call(toolInput, toolContext);
        }

        Map<String, Object> input;

        try {
            input = new LinkedHashMap<>(JsonUtils.readMap(toolInput));
        } catch (JacksonException exception) {
            return delegate.call(toolInput, toolContext);
        }

        if (!(input.get(REQUEST) instanceof String request) || request.contains(userMessage)) {
            return delegate.call(toolInput, toolContext);
        }

        input.put(REQUEST, request + USER_MESSAGE_HEADER + userMessage);

        return delegate.call(JsonUtils.write(input), toolContext);
    }

    private static @Nullable String fetchUserMessage(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        AiHubToolInvocationContext context = AiHubToolInvocationContext.fromToolContext(toolContext);

        if (context == null || context.lastUserPrompt() == null || context.lastUserPrompt()
            .isBlank()) {

            return null;
        }

        return context.lastUserPrompt()
            .strip();
    }
}
