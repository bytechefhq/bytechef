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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A workflow definition is parsed through {@code AbstractWorkflowMapper.validateReservedWords}, which rejects any
 * task-level key that is neither a built-in workflow constant nor contributed by a
 * {@code WorkflowReservedWordContributor} (recursively, for every nested task map — see
 * {@code AbstractWorkflowMapper#validateReservedWords}). {@code AiAgentWorkflowGenerator} writes
 * {@code aiHubWorkspaceId}/{@code aiHubAgentId}/{@code aiHubCreatorUserId} directly onto the generated
 * {@code aiAgent_1} TASK node (ticket 732, {@code 2026-08-17-agent-run-hub-visibility}) — unlike {@code websocketTasks}
 * ({@link WebsocketTasksReservedWordTest}), which sits on a TRIGGER. Without this registration, agent creation/save
 * would fail at {@code WorkflowService.create}/{@code .update} with "unknown workflow definition property" the moment
 * the generator started emitting the stamp — caught once already; this test pins it against a regression.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiHubIdentityStampReservedWordTest {

    @Test
    void testAiHubIdentityStampKeysAreReservedWords() {
        assertThat(new WorkflowReservedWordContributorImpl().getReservedWords())
            .contains(
                WorkflowExtConstants.AI_HUB_WORKSPACE_ID, WorkflowExtConstants.AI_HUB_AGENT_ID,
                WorkflowExtConstants.AI_HUB_CREATOR_USER_ID);
    }

    /**
     * The counterpart to {@code WorkflowReservedWordContributorParityTest} in {@code runtime-job-app}, which registers
     * its own contributor because it is assembled without this module. Both assert against
     * {@link WorkflowExtConstants#RESERVED_WORDS}, so the two implementations cannot drift apart.
     */
    @Test
    void testSharedContributorReturnsTheSingleReservedWordList() {
        assertThat(new WorkflowReservedWordContributorImpl().getReservedWords())
            .containsExactlyInAnyOrderElementsOf(WorkflowExtConstants.RESERVED_WORDS);
    }

    @Test
    void testTaskCarryingAiHubIdentityStampParses() {
        Workflow workflow = new Workflow("workflow-1", DEFINITION, Format.JSON);

        assertThat(workflow.getTasks()).hasSize(1);
    }

    private static final String DEFINITION = """
        {
          "label": "Agent",
          "inputs": [],
          "outputs": [],
          "triggers": [],
          "tasks": [{
            "name": "aiAgent_1",
            "type": "aiAgent/v1/streamChat",
            "parameters": {},
            "aiHubWorkspaceId": 10,
            "aiHubAgentId": 20,
            "aiHubCreatorUserId": 30
          }]
        }""";
}
