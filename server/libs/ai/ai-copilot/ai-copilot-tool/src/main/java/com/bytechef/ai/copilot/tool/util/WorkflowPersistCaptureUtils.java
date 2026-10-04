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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class WorkflowPersistCaptureUtils {

    private static final String PERSISTED_WORKFLOW_CAPTURE_KEY = "bytechef.workflowEditor.persistedWorkflows";

    private WorkflowPersistCaptureUtils() {
    }

    public static Map<String, Object> withCaptureHolder(Map<String, Object> forwardedContext) {
        Map<String, Object> context = new HashMap<>(forwardedContext);

        context.put(PERSISTED_WORKFLOW_CAPTURE_KEY, Collections.synchronizedList(new ArrayList<Map<String, Object>>()));

        return context;
    }

    @SuppressFBWarnings(
        value = "VA_FORMAT_STRING_USES_NEWLINE",
        justification = "The trailer is LLM-facing content that must use a literal \\n, not the platform-dependent %n line separator.")
    public static @Nullable String renderTrailer(Map<String, Object> context) {
        Object holder = context.get(PERSISTED_WORKFLOW_CAPTURE_KEY);

        if (!(holder instanceof List<?> captures) || captures.isEmpty()) {
            return null;
        }

        Object last = captures.get(captures.size() - 1);

        if (!(last instanceof Map<?, ?> entry)) {
            return null;
        }

        Object workflowId = entry.get("workflowId");
        Object projectId = entry.get("projectId");
        Object projectWorkflowId = entry.get("projectWorkflowId");
        Object name = entry.get("name");

        if (workflowId == null || projectId == null || projectWorkflowId == null) {
            return null;
        }

        return """

            ---
            [WORKFLOW PERSISTED — authoritative identifiers. Use these EXACT values; do NOT infer, reformat, or \
            transpose them.] The workflow was saved. Use these exact ids: type="WORKFLOW", workflowId="%s", \
            projectId=%s, projectWorkflowId=%s, name="%s".""".formatted(
            workflowId, projectId, projectWorkflowId, name == null ? "" : name);
    }
}
