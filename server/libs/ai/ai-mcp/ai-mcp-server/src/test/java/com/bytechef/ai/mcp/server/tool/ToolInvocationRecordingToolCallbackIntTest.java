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

package com.bytechef.ai.mcp.server.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import com.bytechef.platform.tool.execution.config.ToolExecutionConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringJUnitConfig(
    classes = {
        ToolExecutionConfiguration.class,
        ToolInvocationRecordingToolCallbackIntTest.ToolInvocationRecordingToolCallbackIntTestConfiguration.class
    })
class ToolInvocationRecordingToolCallbackIntTest {

    @Autowired
    private ToolExecutionEventCollector toolExecutionEventCollector;

    @Autowired
    private ToolExecutionRecorder toolExecutionRecorder;

    @Autowired
    private WorkspaceFacade workspaceFacade;

    @BeforeEach
    void beforeEach() {
        reset(workspaceFacade);

        toolExecutionEventCollector.reset();

        when(workspaceFacade.getMyWorkspaces()).thenReturn(
            List.of(new Workspace(3L, "Sales"), new Workspace(4L, "Support")));
    }

    @Test
    void testRecordsSuccessfulCallInRequestedWorkspace() {
        ToolCallback toolCallback = recording(
            "listProjects", ToolExecutionKind.MANAGEMENT_TOOL, toolInput -> "[{\"id\":1}]");

        String result = toolCallback.call("{\"workspaceId\":4}");

        assertThat(result).isEqualTo("[{\"id\":1}]");

        ToolExecutionEvent event = toolExecutionEventCollector.getSingleEvent();

        assertThat(event.type()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(event.surface()).isEqualTo(ToolExecutionSurface.MCP_MANAGEMENT);
        assertThat(event.kind()).isEqualTo(ToolExecutionKind.MANAGEMENT_TOOL);
        assertThat(event.toolName()).isEqualTo("listProjects");
        assertThat(event.workspaceId()).isEqualTo(4L);
        assertThat(event.environment()).isEqualTo(Environment.DEVELOPMENT.ordinal());
        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.SUCCESS);
    }

    @Test
    void testRecordsToolContextWorkspaceAndRequestedEnvironment() {
        ToolCallback toolCallback = recording(
            "listProjectDeployments", ToolExecutionKind.CONTRIBUTED, toolInput -> "[]");

        toolCallback.call(
            "{\"environment\":\"production\"}",
            new ToolContext(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 3L)));

        ToolExecutionEvent event = toolExecutionEventCollector.getSingleEvent();

        assertThat(event.kind()).isEqualTo(ToolExecutionKind.CONTRIBUTED);
        assertThat(event.workspaceId()).isEqualTo(3L);
        assertThat(event.environment()).isEqualTo(Environment.PRODUCTION.ordinal());
    }

    @Test
    void testRecordsCallWithoutWorkspaceWhenNoneCanBeResolved() {
        when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(3L, "Sales")));

        ToolCallback toolCallback = recording(
            "listProjects", ToolExecutionKind.MANAGEMENT_TOOL, toolInput -> "{\"error\":\"workspace_required\"}");

        toolCallback.call("{}");

        ToolExecutionEvent event = toolExecutionEventCollector.getSingleEvent();

        assertThat(event.workspaceId()).isNull();
        assertThat(event.environment()).isNull();
    }

    @Test
    void testRecordsCallWithoutWorkspaceWhenRequestedWorkspaceIsNotAccessible() {
        ToolCallback toolCallback = recording("listComponents", ToolExecutionKind.MANAGEMENT_TOOL, toolInput -> "[]");

        toolCallback.call("{\"workspaceId\":9}");

        ToolExecutionEvent event = toolExecutionEventCollector.getSingleEvent();

        assertThat(event.workspaceId()).isNull();
        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.SUCCESS);
    }

    @Test
    void testRecordsErrorResultAsError() {
        ToolCallback toolCallback = recording(
            "getProject", ToolExecutionKind.MANAGEMENT_TOOL,
            toolInput -> "{\"error\":\"Project 9 not found in workspace 3\"}");

        String result = toolCallback.call("{\"workspaceId\":3,\"projectId\":9}");

        assertThat(result).isEqualTo("{\"error\":\"Project 9 not found in workspace 3\"}");

        ToolExecutionEvent event = toolExecutionEventCollector.getSingleEvent();

        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.ERROR);
    }

    @Test
    void testRecordsResultMentioningErrorInsideValueAsSuccess() {
        ToolCallback toolCallback = recording(
            "getWorkflow", ToolExecutionKind.MANAGEMENT_TOOL,
            toolInput -> "{\"definition\":\"{\\\"error\\\":1}\"}");

        toolCallback.call("{\"workspaceId\":3}");

        assertThat(toolExecutionEventCollector.getSingleEvent()
            .outcome()).isEqualTo(ToolExecutionOutcome.SUCCESS);
    }

    @Test
    void testRecordsThrownFailureAndRethrows() {
        ToolCallback toolCallback = recording(
            "deleteProject", ToolExecutionKind.MANAGEMENT_TOOL, toolInput -> {
                throw new IllegalStateException("boom");
            });

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> toolCallback.call("{\"workspaceId\":3}"))
            .withMessage("boom");

        ToolExecutionEvent event = toolExecutionEventCollector.getSingleEvent();

        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.ERROR);
        assertThat(event.errorType()).isEqualTo("IllegalStateException");
        assertThat(event.errorMessage()).isEqualTo("boom");
        assertThat(event.workspaceId()).isEqualTo(3L);
    }

    @Test
    void testPassesToolContextAndExposesDelegateDefinition() {
        ToolContext toolContext = new ToolContext(Map.of("key", "value"));
        StubToolCallback delegate = new StubToolCallback("listProjects", toolInput -> "[]");

        ToolCallback toolCallback = new ToolInvocationRecordingToolCallback(
            delegate, ToolExecutionKind.MANAGEMENT_TOOL, toolExecutionRecorder, workspaceFacade);

        toolCallback.call("{\"workspaceId\":3}", toolContext);

        assertThat(delegate.lastToolContext).isSameAs(toolContext);
        assertThat(toolCallback.getToolDefinition()).isSameAs(delegate.getToolDefinition());
    }

    private ToolCallback recording(String toolName, ToolExecutionKind kind, Function<String, String> function) {
        return new ToolInvocationRecordingToolCallback(
            new StubToolCallback(toolName, function), kind, toolExecutionRecorder, workspaceFacade);
    }

    private static final class StubToolCallback implements ToolCallback {

        private final Function<String, String> function;
        private final ToolDefinition toolDefinition;

        private @Nullable ToolContext lastToolContext;

        private StubToolCallback(String toolName, Function<String, String> function) {
            this.function = function;
            this.toolDefinition = ToolDefinition.builder()
                .name(toolName)
                .description(toolName)
                .inputSchema("{\"type\":\"object\"}")
                .build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        @Override
        public String call(String toolInput) {
            return call(toolInput, null);
        }

        @Override
        public String call(String toolInput, @Nullable ToolContext toolContext) {
            lastToolContext = toolContext;

            return function.apply(toolInput);
        }
    }

    static class ToolExecutionEventCollector {

        private final List<ToolExecutionEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        void onToolExecutionEvent(ToolExecutionEvent event) {
            events.add(event);
        }

        ToolExecutionEvent getSingleEvent() {
            assertThat(events).hasSize(1);

            return events.getFirst();
        }

        void reset() {
            events.clear();
        }
    }

    @Configuration
    static class ToolInvocationRecordingToolCallbackIntTestConfiguration {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        ToolExecutionEventCollector toolExecutionEventCollector() {
            return new ToolExecutionEventCollector();
        }

        @Bean
        WorkspaceFacade workspaceFacade() {
            return mock(WorkspaceFacade.class);
        }
    }
}
