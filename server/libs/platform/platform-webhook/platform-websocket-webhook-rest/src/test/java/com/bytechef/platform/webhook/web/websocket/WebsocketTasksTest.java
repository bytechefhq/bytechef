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

package com.bytechef.platform.webhook.web.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pins that BOTH placements of {@code websocketTasks} resolve. The canonical one is a trigger extension; the
 * {@code parameters} placement shipped in {@code docs/examples/voice/deepgram-voiceagent.json} and in the client's
 * voice-capability check, so dropping it would break working voice workflows.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WebsocketTasksTest {

    private static final String DEFINITION = "{\"tasks\":[{\"name\":\"agent\",\"type\":\"deepgram/v1/voiceAgent\"}]}";

    @Test
    void testResolvesFromExtension() {
        WorkflowTrigger workflowTrigger = createWorkflowTrigger(DEFINITION, null);

        assertThat(WebsocketTasks.resolve(workflowTrigger)).isEqualTo(DEFINITION);
    }

    @Test
    void testResolvesFromParameters() {
        WorkflowTrigger workflowTrigger = createWorkflowTrigger(null, DEFINITION);

        assertThat(WebsocketTasks.resolve(workflowTrigger)).isEqualTo(DEFINITION);
    }

    @Test
    void testExtensionWinsOverParameters() {
        WorkflowTrigger workflowTrigger = createWorkflowTrigger(DEFINITION, "{\"tasks\":[]}");

        assertThat(WebsocketTasks.resolve(workflowTrigger)).isEqualTo(DEFINITION);
    }

    @Test
    void testReturnsNullWhenAbsent() {
        assertThat(WebsocketTasks.resolve(createWorkflowTrigger(null, null))).isNull();
    }

    @Test
    void testBlankExtensionFallsBackToParameters() {
        WorkflowTrigger workflowTrigger = createWorkflowTrigger("   ", DEFINITION);

        assertThat(WebsocketTasks.resolve(workflowTrigger)).isEqualTo(DEFINITION);
    }

    @Test
    void testReturnsNullWhenBothBlank() {
        assertThat(WebsocketTasks.resolve(createWorkflowTrigger("", "   "))).isNull();
    }

    private static WorkflowTrigger createWorkflowTrigger(String extensionValue, String parameterValue) {
        Map<String, Object> source = new LinkedHashMap<>();

        source.put("name", "trigger_1");
        source.put("type", "browser/v1/voiceSession/v1");

        if (extensionValue != null) {
            source.put(WebsocketTasks.WEBSOCKET_TASKS, extensionValue);
        }

        Map<String, Object> parameters = new LinkedHashMap<>();

        if (parameterValue != null) {
            parameters.put(WebsocketTasks.WEBSOCKET_TASKS, parameterValue);
        }

        source.put("parameters", parameters);

        return new WorkflowTrigger(source);
    }
}
