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

package com.bytechef.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.tool.util.WorkflowPersistCaptureUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowPersistCaptureUtilsTest {

    // Mirrors the (package-private) constant in WorkflowPersistCapture and the duplicated key in ProjectWorkflowTools.
    private static final String CAPTURE_KEY = "bytechef.workflowEditor.persistedWorkflows";

    @Test
    void testWithCaptureHolderInstallsMutableListWithoutMutatingSource() {
        Map<String, Object> source = Map.of("bytechef.agentTool.environmentId", 2L);

        Map<String, Object> context = WorkflowPersistCaptureUtils.withCaptureHolder(source);

        assertThat(context)
            .containsEntry("bytechef.agentTool.environmentId", 2L)
            .containsKey(CAPTURE_KEY);
        assertThat(context.get(CAPTURE_KEY)).isInstanceOf(List.class);
        assertThat(source).doesNotContainKey(CAPTURE_KEY);
    }

    @Test
    void testRenderTrailerReturnsNullWhenNothingPersisted() {
        Map<String, Object> context = WorkflowPersistCaptureUtils.withCaptureHolder(Map.of());

        assertThat(WorkflowPersistCaptureUtils.renderTrailer(context)).isNull();
    }

    @Test
    void testRenderTrailerReturnsNullWhenHolderMissing() {
        assertThat(WorkflowPersistCaptureUtils.renderTrailer(Map.of())).isNull();
    }

    @Test
    void testRenderTrailerReturnsNullWhenProjectWorkflowIdMissing() {
        Map<String, Object> context = WorkflowPersistCaptureUtils.withCaptureHolder(Map.of());

        capture(context, "wf-uuid-1", 7L, null, "My Flow");

        assertThat(WorkflowPersistCaptureUtils.renderTrailer(context)).isNull();
    }

    @Test
    void testRenderTrailerCarriesExactPersistedIds() {
        Map<String, Object> context = WorkflowPersistCaptureUtils.withCaptureHolder(Map.of());

        capture(context, "wf-uuid-1", 7L, 55L, "My Flow");

        String trailer = WorkflowPersistCaptureUtils.renderTrailer(context);

        assertThat(trailer)
            .isNotNull()
            .contains("openResourceTab")
            .contains("workflowId=\"wf-uuid-1\"")
            .contains("projectId=7")
            .contains("projectWorkflowId=55")
            .contains("name=\"My Flow\"");
    }

    @Test
    void testRenderTrailerUsesMostRecentPersist() {
        Map<String, Object> context = WorkflowPersistCaptureUtils.withCaptureHolder(Map.of());

        capture(context, "wf-uuid-1", 7L, 55L, "First");
        capture(context, "wf-uuid-2", 7L, 56L, "Second");

        String trailer = WorkflowPersistCaptureUtils.renderTrailer(context);

        assertThat(trailer)
            .contains("projectWorkflowId=56")
            .contains("name=\"Second\"");
    }

    // Mirrors what ProjectWorkflowTools.capturePersistedWorkflow writes into the holder at persist time.
    @SuppressWarnings("unchecked")
    private static void capture(
        Map<String, Object> context, String workflowId, long projectId, Long projectWorkflowId, String name) {

        List<Map<String, Object>> captures =
            (List<Map<String, Object>>) context.get(CAPTURE_KEY);

        Map<String, Object> entry = new HashMap<>();

        entry.put("created", true);
        entry.put("workflowId", workflowId);
        entry.put("projectId", projectId);
        entry.put("projectWorkflowId", projectWorkflowId);
        entry.put("name", name);

        captures.add(entry);
    }
}
