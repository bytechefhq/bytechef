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

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Validates a workflow the build specialist saved, with the same checks the workflow editor's issue panel runs.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface PersistedWorkflowChecker {

    List<Issue> check(String workflowId);

    /**
     * One validation error of the saved workflow. {@code userAction} marks an error only the user can resolve, such as
     * a connection the workflow still needs.
     */
    record Issue(@Nullable String nodeName, String message, boolean userAction) {
    }
}
