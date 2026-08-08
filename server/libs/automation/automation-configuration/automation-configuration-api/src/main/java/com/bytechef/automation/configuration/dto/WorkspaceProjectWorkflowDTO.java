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

package com.bytechef.automation.configuration.dto;

/**
 * A workspace-wide, flat listing of one project workflow: just enough to render a picker entry and open the workflow's
 * tab, without the workflow definition itself.
 *
 * <p>
 * Deliberately narrower than {@link ProjectWorkflowDTO}, which carries the parsed {@code Workflow} (definition, inputs,
 * outputs, tasks). Callers that list every workflow in a workspace only need a label and the ids, and parsing a
 * definition per workflow purely to read its label is what made the per-project listing expensive enough that clients
 * fanned out one request per project instead.
 * </p>
 *
 * @author Ivica Cardic
 */
public record WorkspaceProjectWorkflowDTO(
    long projectId, String projectName, long projectWorkflowId, String workflowId, String workflowLabel) {
}
