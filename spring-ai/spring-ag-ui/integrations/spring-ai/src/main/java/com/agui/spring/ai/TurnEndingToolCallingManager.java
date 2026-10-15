package com.agui.spring.ai;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.function.Predicate;

public class TurnEndingToolCallingManager implements ToolCallingManager {

    private final ToolCallingManager delegate;
    private final Predicate<ToolResponseMessage.ToolResponse> turnEndingToolResponse;

    public TurnEndingToolCallingManager(
        ToolCallingManager delegate, Predicate<ToolResponseMessage.ToolResponse> turnEndingToolResponse) {

        this.delegate = delegate;
        this.turnEndingToolResponse = turnEndingToolResponse;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        ToolExecutionResult toolExecutionResult = delegate.executeToolCalls(prompt, chatResponse);

        if (toolExecutionResult.returnDirect() || !endsTurn(toolExecutionResult)) {
            return toolExecutionResult;
        }

        return ToolExecutionResult.builder()
            .conversationHistory(toolExecutionResult.conversationHistory())
            .returnDirect(true)
            .build();
    }

    private boolean endsTurn(ToolExecutionResult toolExecutionResult) {
        List<Message> conversationHistory = toolExecutionResult.conversationHistory();

        if (conversationHistory.isEmpty()) {
            return false;
        }

        if (!(conversationHistory.getLast() instanceof ToolResponseMessage toolResponseMessage)) {
            return false;
        }

        return toolResponseMessage.getResponses()
            .stream()
            .anyMatch(turnEndingToolResponse);
    }
}
