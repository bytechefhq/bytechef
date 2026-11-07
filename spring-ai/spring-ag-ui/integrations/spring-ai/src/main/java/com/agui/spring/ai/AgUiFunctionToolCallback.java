package com.agui.spring.ai;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class AgUiFunctionToolCallback implements ToolCallback {

    public static final String TOOL_CALL_ID_KEY = "agUiToolCallId";

    private final ToolCallback toolCallback;

    private final AgUiToolCallListener listener;

    public AgUiFunctionToolCallback(final ToolCallback toolCallback, final AgUiToolCallListener listener) {
        this.toolCallback = toolCallback;

        this.listener = listener;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolCallback.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    public String call(String toolInput, @Nullable ToolContext toolContext) {
        var toolCallId = UUID.randomUUID().toString();

        this.listener.onToolCallStarted(toolCallId, getToolDefinition().name(), toolInput);

        String result;

        try {
            result = this.toolCallback.call(toolInput, withToolCallId(toolContext, toolCallId));
        } catch (RuntimeException | Error throwable) {
            this.listener.onToolCallFailed(toolCallId, throwable);

            throw throwable;
        }

        this.listener.onToolCallFinished(toolCallId, result);

        return result;
    }

    private static ToolContext withToolCallId(@Nullable ToolContext toolContext, String toolCallId) {
        if (toolContext == null) {
            return null;
        }

        var context = new HashMap<>(toolContext.getContext());

        context.put(TOOL_CALL_ID_KEY, toolCallId);

        return new ToolContext(context);
    }

}
