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

package com.bytechef.automation.configuration.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ScheduledWorkflowCheckerTest {

    private static final String CREATE_RECORDS_TASK =
        "{\"name\": \"dataTable_1\", \"type\": \"dataTable/v1/createRecords\", \"parameters\": {\"table\": \"orders\"}}";
    private static final String UPDATE_RECORD_TASK =
        "{\"name\": \"dataTable_1\", \"type\": \"dataTable/v1/updateRecord\", \"parameters\": {\"table\": \"orders\"}}";

    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final ScheduledWorkflowChecker scheduledWorkflowChecker = new ScheduledWorkflowChecker(
        projectWorkflowService, workflowService);

    @Test
    void testKindIsScheduled() {
        assertThat(scheduledWorkflowChecker.kind()).isEqualTo(WorkflowKind.SCHEDULED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "schedule/v1/cron", "schedule/v1/everyDay", "schedule/v1/everyWeek", "schedule/v1/everyMonth",
        "schedule/v1/interval"
    })
    void testAScheduleTriggerWritingToADataTablePasses(String triggerType) {
        stubWorkflow(triggerType, CREATE_RECORDS_TASK);

        CallableWorkflowCheck callableWorkflowCheck = scheduledWorkflowChecker.check("wf-1");

        assertThat(callableWorkflowCheck.callable()).isTrue();
        assertThat(callableWorkflowCheck.problems()).isEmpty();
    }

    @Test
    void testAnUpdateRecordTaskNestedInABranchCountsAsAWrite() {
        stubWorkflow(
            "schedule/v1/everyDay",
            "{\"name\": \"condition_1\", \"type\": \"condition/v1\", \"parameters\": {\"caseTrue\": [" +
                UPDATE_RECORD_TASK + "], \"caseFalse\": []}}");

        assertThat(scheduledWorkflowChecker.check("wf-1")
            .callable()).isTrue();
    }

    @Test
    void testANonScheduleTriggerIsNamed() {
        stubWorkflow("manual/v1/manual", CREATE_RECORDS_TASK);

        assertThat(scheduledWorkflowChecker.check("wf-1")
            .problems()).containsExactly("trigger is manual/v1/manual, not a schedule trigger");
    }

    @Test
    void testAWorkflowWithoutATriggerIsNamed() {
        when(workflowService.fetchWorkflow("wf-1")).thenReturn(
            Optional.of(new Workflow("wf-1", "{\"tasks\": [" + CREATE_RECORDS_TASK + "]}", Workflow.Format.JSON)));

        assertThat(scheduledWorkflowChecker.check("wf-1")
            .problems()).containsExactly("trigger is none, not a schedule trigger");
    }

    @Test
    void testAScheduleWorkflowThatWritesNowhereReportsTheMissingDataTableWrite() {
        stubWorkflow(
            "schedule/v1/cron",
            "{\"name\": \"dataTable_1\", \"type\": \"dataTable/v1/getRecords\", \"parameters\": {}}");

        assertThat(scheduledWorkflowChecker.check("wf-1")
            .problems()).containsExactly("no dataTable write task");
    }

    @Test
    void testBothProblemsAreReported() {
        stubWorkflow("webhook/v1/autoRespondWithHTTP200", null);

        assertThat(scheduledWorkflowChecker.check("wf-1")
            .problems()).containsExactly(
                "trigger is webhook/v1/autoRespondWithHTTP200, not a schedule trigger", "no dataTable write task");
    }

    @Test
    void testAMissingWorkflowIsNamed() {
        when(workflowService.fetchWorkflow("wf-9")).thenReturn(Optional.empty());

        assertThat(scheduledWorkflowChecker.check("wf-9")
            .problems()).containsExactly("workflow wf-9 does not exist");
    }

    @Test
    void testFetchWorkflowUuidReturnsTheProjectWorkflowUuid() {
        UUID uuid = UUID.fromString("0b9f0b0e-5f1e-4d0a-8c52-2c3a0a8d8b11");

        when(projectWorkflowService.fetchWorkflowProjectWorkflow("wf-1"))
            .thenReturn(Optional.of(new ProjectWorkflow(1L, 1, "wf-1", uuid)));

        assertThat(scheduledWorkflowChecker.fetchWorkflowUuid("wf-1")).contains(uuid.toString());
    }

    @Test
    void testFetchWorkflowUuidIsEmptyForANonProjectWorkflow() {
        when(projectWorkflowService.fetchWorkflowProjectWorkflow("wf-1")).thenReturn(Optional.empty());

        assertThat(scheduledWorkflowChecker.fetchWorkflowUuid("wf-1")).isEmpty();
    }

    private void stubWorkflow(String triggerType, String task) {
        String tasks = task == null ? "" : task;

        when(workflowService.fetchWorkflow("wf-1")).thenReturn(
            Optional.of(
                new Workflow(
                    "wf-1",
                    """
                        {"triggers": [{"name": "trigger_1", "type": "%s", "parameters": {}}], "tasks": [%s]}\
                        """.formatted(triggerType, tasks),
                    Workflow.Format.JSON)));
    }
}
