package com.agui.spring.ai;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.Nullable;

import java.util.UUID;

public class AgUiFunctionToolCallback implements ToolCallback {

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
            result = this.toolCallback.call(toolInput, toolContext);
        } catch (RuntimeException | Error throwable) {
            this.listener.onToolCallFailed(toolCallId, throwable);

            throw throwable;
        }

        this.listener.onToolCallFinished(toolCallId, result);

        return result;
    }

}
