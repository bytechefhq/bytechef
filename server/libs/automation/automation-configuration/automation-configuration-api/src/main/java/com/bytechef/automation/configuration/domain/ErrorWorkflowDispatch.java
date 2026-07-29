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

package com.bytechef.automation.configuration.domain;

/**
 * Everything the coordinator needs to dispatch a handler: the workflow id to submit, plus the failed run's identity for
 * the payload. The {@code failed*} fields describe the run that failed, not the handler — the payload's workflow block
 * identifies the failed workflow, as it does in n8n.
 * <p>
 * {@code environment} is read from the failed run's {@code ProjectDeployment} -- never from job metadata, which nothing
 * in this codebase populates -- so it carries a real value (or {@code null}) rather than the literal string
 * {@code "null"}.
 * <p>
 * {@code errorTriggerName} is the handler workflow's {@code workflow/newWorkflowError} trigger node name. ByteChef
 * exposes a trigger's output to the rest of the workflow under that node name (see {@code TriggerCompletionHandler}),
 * and editor data pills are emitted node-name-prefixed ({@code ${<triggerName>.execution.jobId}}). The dispatched
 * payload must be nested under this name -- not passed as top-level inputs -- or every pill in a handler built in the
 * editor resolves to null.
 *
 * @author Ivica Cardic
 */
public record ErrorWorkflowDispatch(
    String handlerWorkflowId, long projectId, long failedProjectWorkflowId, String failedWorkflowId,
    String failedWorkflowLabel, String environment, String errorTriggerName) {
}
