/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolBinding;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolFacade;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ListTaskToolsToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testListsAttachedTools() throws Exception {
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        AiHubTaskToolFacade taskToolFacade = mock(AiHubTaskToolFacade.class);

        AiHubTask task = mock(AiHubTask.class);

        when(task.getId()).thenReturn(7L);
        when(taskService.findByThreadId("thread-1")).thenReturn(Optional.of(task));

        AiHubTaskToolBinding binding = new AiHubTaskToolBinding(
            11L, 99L, 7L, "slack", 1, "sendMessage", 42L, 0, Map.of("channel", "#engineering"));

        when(taskToolFacade.listTaskTools(7L)).thenReturn(List.of(binding));

        ListTaskToolsToolCallback callback =
            new ListTaskToolsToolCallback(taskService, taskToolFacade, mock(AiHubToolAttachMetrics.class), jsonMapper);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("tools")
            .size()).isEqualTo(1);
        assertThat(node.get("tools")
            .get(0)
            .get("componentName")
            .asText()).isEqualTo("slack");
        assertThat(node.get("tools")
            .get(0)
            .get("actionName")
            .asText()).isEqualTo("sendMessage");
        assertThat(node.get("tools")
            .get(0)
            .get("connectionId")
            .asLong()).isEqualTo(42L);
    }

    @Test
    void testReturnsEmptyToolsArrayWhenNoneAttached() throws Exception {
        AiHubTaskService taskService = mock(AiHubTaskService.class);
        AiHubTaskToolFacade taskToolFacade = mock(AiHubTaskToolFacade.class);

        AiHubTask task = mock(AiHubTask.class);

        when(task.getId()).thenReturn(7L);
        when(taskService.findByThreadId("thread-1")).thenReturn(Optional.of(task));

        when(taskToolFacade.listTaskTools(7L)).thenReturn(List.of());

        ListTaskToolsToolCallback callback =
            new ListTaskToolsToolCallback(taskService, taskToolFacade, mock(AiHubToolAttachMetrics.class), jsonMapper);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("tools")
            .isEmpty()).isTrue();
    }

    @Test
    void testReturnsToolErrorWhenContextMissing() throws Exception {
        ListTaskToolsToolCallback callback = new ListTaskToolsToolCallback(
            mock(AiHubTaskService.class), mock(AiHubTaskToolFacade.class), mock(AiHubToolAttachMetrics.class),
            jsonMapper);

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("AiHubTask context unavailable");
    }

    @Test
    void testReturnsToolErrorWhenTaskNotFound() throws Exception {
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        when(taskService.findByThreadId("missing")).thenReturn(Optional.empty());

        ListTaskToolsToolCallback callback = new ListTaskToolsToolCallback(
            taskService, mock(AiHubTaskToolFacade.class), mock(AiHubToolAttachMetrics.class), jsonMapper);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "missing").toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("AiHubTask not found");
    }
}
