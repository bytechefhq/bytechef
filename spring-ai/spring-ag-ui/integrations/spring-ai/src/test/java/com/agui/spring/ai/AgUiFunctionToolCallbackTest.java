package com.agui.spring.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AgUiFunctionToolCallbackTest {

    @Test
    void shouldPassTheStartedToolCallIdToTheToolInItsContext() {
        var startedToolCallIds = new CopyOnWriteArrayList<String>();
        var receivedContext = new AtomicReference<ToolContext>();

        var toolCallback = new AgUiFunctionToolCallback(
            new RecordingToolCallback(receivedContext), new RecordingListener(startedToolCallIds));

        var result = toolCallback.call("{}", new ToolContext(Map.of("tenantId", "acme")));

        assertThat(result).isEqualTo("ok");
        assertThat(startedToolCallIds).hasSize(1);
        assertThat(receivedContext.get().getContext())
            .containsEntry("tenantId", "acme")
            .containsEntry(AgUiFunctionToolCallback.TOOL_CALL_ID_KEY, startedToolCallIds.getFirst());
    }

    @Test
    void shouldLeaveAMissingToolContextMissing() {
        var receivedContext = new AtomicReference<ToolContext>(new ToolContext(Map.of()));

        var toolCallback = new AgUiFunctionToolCallback(
            new RecordingToolCallback(receivedContext), new RecordingListener(new CopyOnWriteArrayList<>()));

        toolCallback.call("{}", null);

        assertThat(receivedContext.get()).isNull();
    }

    private record RecordingListener(List<String> startedToolCallIds) implements AgUiToolCallListener {

        @Override
        public void onToolCallStarted(String toolCallId, String toolName, String arguments) {
            startedToolCallIds.add(toolCallId);
        }

        @Override
        public void onToolCallFinished(String toolCallId, String result) {
        }

        @Override
        public void onToolCallFailed(String toolCallId, Throwable throwable) {
        }
    }

    private record RecordingToolCallback(AtomicReference<ToolContext> receivedContext) implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                .name("searchTasks")
                .description("Search the tasks")
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build();
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            receivedContext.set(toolContext);

            return "ok";
        }
    }
}
