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

package com.bytechef.platform.configuration.workflow.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A workflow definition is parsed through {@code AbstractWorkflowMapper.validateReservedWords}, which rejects any
 * trigger-level key that is neither a built-in workflow constant nor contributed by a
 * {@code WorkflowReservedWordContributor}. That is why {@code websocketTasks} — the embedded voice pipeline, a
 * structural key rather than a component-declared trigger property — has to be registered: without it, the canonical
 * placement does not parse at all and authors are pushed into hiding the pipeline inside {@code parameters}, which is
 * not validated.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WebsocketTasksReservedWordTest {

    private static final String PIPELINE =
        "{\\\"tasks\\\":[{\\\"name\\\":\\\"agent\\\",\\\"type\\\":\\\"deepgram/v1/voiceAgent/v1\\\"}]}";

    @Test
    void testWebsocketTasksIsAReservedWord() {
        assertThat(new WorkflowReservedWordContributorImpl().getReservedWords())
            .contains(WorkflowExtConstants.WEBSOCKET_TASKS);
    }

    @Test
    void testTriggerCarryingWebsocketTasksParses() {
        Workflow workflow =
            new Workflow("workflow-1", createDefinition(WorkflowExtConstants.WEBSOCKET_TASKS), Format.JSON);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        assertThat(workflowTriggers).hasSize(1);

        WorkflowTrigger workflowTrigger = workflowTriggers.getFirst();

        assertThat(workflowTrigger.getExtension(WorkflowExtConstants.WEBSOCKET_TASKS, String.class, null))
            .isNotBlank();
    }

    @Test
    void testUnregisteredTriggerKeyStillFails() {
        // Guards the premise of the test above: parsing really does reject unknown trigger keys, so the passing
        // case is the contributor doing its job and not validation having been relaxed.
        assertThatThrownBy(() -> new Workflow("workflow-1", createDefinition("websocketTasksTypo"), Format.JSON))
            .hasMessageContaining("unknown workflow definition property");
    }

    private static String createDefinition(String triggerKey) {
        return """
            {
              "label": "Voice",
              "inputs": [],
              "outputs": [],
              "triggers": [{
                "name": "trigger_1",
                "type": "browser/v1/voiceSession/v1",
                "TRIGGER_KEY": "PIPELINE",
                "parameters": {"sampleRate": "16000"}
              }],
              "tasks": []
            }"""
            .replace("TRIGGER_KEY", triggerKey)
            .replace("PIPELINE", PIPELINE);
    }
}
