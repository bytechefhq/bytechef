package com.agui.spring.ai;

/**
 * Receives the lifecycle of one tool call that {@link AgUiFunctionToolCallback} runs, keyed by the tool call id it
 * generates before the tool executes.
 */
public interface AgUiToolCallListener {

    void onToolCallStarted(String toolCallId, String toolName, String arguments);

    void onToolCallFinished(String toolCallId, String result);

    void onToolCallFailed(String toolCallId, RuntimeException exception);
}
