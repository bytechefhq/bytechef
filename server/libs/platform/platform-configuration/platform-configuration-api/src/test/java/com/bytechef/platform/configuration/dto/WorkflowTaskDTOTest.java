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

package com.bytechef.platform.configuration.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTaskDTOTest {

    @Test
    void testComponentNamesOfATaskAreItsOwnComponent() {
        WorkflowTaskDTO workflowTaskDTO = workflowTaskDTO(
            "slack/v1/sendChannelMessage", List.of(new ComponentConnection("slack", 1, "slack_1", "slack", true)));

        assertThat(workflowTaskDTO.getComponentNames()).containsExactly("slack");
    }

    @Test
    void testComponentNamesOfACodeTaskAreTheComponentsItDeclaresConnectionsTo() {
        WorkflowTaskDTO workflowTaskDTO = workflowTaskDTO(
            "codeWorkflow/v1/perform",
            List.of(
                new ComponentConnection("slack", 1, "postToSlack", "slack", true),
                new ComponentConnection("googleMail", 1, "postToSlack", "googleMail", true)));

        assertThat(workflowTaskDTO.getComponentNames()).containsExactly("slack", "googleMail");
    }

    @Test
    void testComponentNamesOfACodeTaskWithoutConnectionsAreEmpty() {
        assertThat(workflowTaskDTO("codeWorkflow/v1/perform", List.of()).getComponentNames()).isEmpty();
    }

    private static WorkflowTaskDTO workflowTaskDTO(String type, List<ComponentConnection> connections) {
        WorkflowTask workflowTask = new WorkflowTask(Map.of("name", "task_1", "type", type));

        return new WorkflowTaskDTO(workflowTask, false, ClusterElementMap.of(Map.of()), connections);
    }
}
