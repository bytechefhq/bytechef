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

package com.bytechef.ai.copilot.tool.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowPersistCaptureUtilsTest {

    private static final String WORKFLOW_ID = "c8a33048-2562-4fba-baae-f353aa13551c";

    @Test
    void testWithTargetWorkflowIdRecordsTheOneWorkflowTheRequestNames() {
        Map<String, Object> context = WorkflowPersistCaptureUtils.withTargetWorkflowId(
            new HashMap<>(), "Workflow " + WORKFLOW_ID + " in project 1160: build a chat agent.");

        assertThat(WorkflowPersistCaptureUtils.fetchTargetWorkflowId(context)).isEqualTo(WORKFLOW_ID);
    }

    @Test
    void testWithTargetWorkflowIdRecordsNothingWhenTheRequestNamesTwoWorkflows() {
        Map<String, Object> context = WorkflowPersistCaptureUtils.withTargetWorkflowId(
            new HashMap<>(), "Copy " + WORKFLOW_ID + " into 7def9ada-e86c-4a40-b690-d693cdce6560.");

        assertThat(WorkflowPersistCaptureUtils.fetchTargetWorkflowId(context)).isNull();
    }

    @Test
    void testWithTargetWorkflowIdRecordsTheWorkflowALabelledLeadingLineNames() {
        Map<String, Object> context = WorkflowPersistCaptureUtils.withTargetWorkflowId(
            new HashMap<>(), "Target workflowId: " + WORKFLOW_ID + "\n\nAdd a Slack notification.");

        assertThat(WorkflowPersistCaptureUtils.fetchTargetWorkflowId(context)).isEqualTo(WORKFLOW_ID);
    }

    @Test
    void testWithTargetWorkflowIdRecordsNothingWhenTheRequestOnlyMentionsAWorkflow() {
        Map<String, Object> context = WorkflowPersistCaptureUtils.withTargetWorkflowId(
            new HashMap<>(),
            "Create a new workflow in project 1160 that calls workflow " + WORKFLOW_ID + " as a subflow.");

        assertThat(WorkflowPersistCaptureUtils.fetchTargetWorkflowId(context)).isNull();
    }

    @Test
    void testWithTargetWorkflowIdRecordsNothingWhenTheRequestNamesNoWorkflow() {
        Map<String, Object> context = WorkflowPersistCaptureUtils.withTargetWorkflowId(
            new HashMap<>(), "Create a new workflow in project 1160.");

        assertThat(WorkflowPersistCaptureUtils.fetchTargetWorkflowId(context)).isNull();
    }
}
