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

/**
 * Answers whether a workflow can be called by an app: triggered by {@code workflow/v1/newWorkflowCall} with an
 * {@code inputSchema} that is absent or readable JSON Schema. The implementation is EE
 * ({@code AppCallableWorkflowResolver}), so binding, listing, execution and this check share one definition; CE code
 * receives it through an {@code ObjectProvider} and treats its absence as "no app-callable checks on this edition".
 *
 * @author Ivica Cardic
 */
public interface CallableWorkflowChecker {

    CallableWorkflowCheck check(String workflowId);
}
