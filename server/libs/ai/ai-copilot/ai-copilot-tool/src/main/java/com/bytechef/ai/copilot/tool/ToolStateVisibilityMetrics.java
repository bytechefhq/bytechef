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

/**
 * Surface-neutral metric sink for shared tool callbacks: records a tool's state-visibility outcome (e.g. "success",
 * "empty", "connection_required").
 *
 * @author Ivica Cardic
 */
public interface ToolStateVisibilityMetrics {

    ToolStateVisibilityMetrics NOOP = (toolName, state) -> {};

    void recordStateVisibility(String toolName, String state);

    /**
     * Records an {@code askUserQuestion} outcome to a dedicated metric where the surface tracks one; default is a no-op
     * for surfaces without a dedicated ask-user-question counter.
     */
    default void recordAskUserQuestion(String outcome) {
    }
}
