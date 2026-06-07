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

package com.bytechef.platform.webhook.executor;

import org.jspecify.annotations.Nullable;

/**
 * Typed envelope for
 * {@link WebhookWorkflowExecutionFacade#executeSync(com.bytechef.platform.workflow.WorkflowExecutionId, com.bytechef.platform.component.trigger.WebhookRequest)}
 * results. Lets non-HTTP transports (e.g. the AI copilot webhook bridge) consume the facade without having to
 * round-trip through {@link org.springframework.http.ResponseEntity}.
 *
 * <p>
 * Two variants:
 * </p>
 * <ul>
 * <li>{@code Ok(outputs)} — workflow completed; {@code outputs} is whatever the workflow returned (typically a
 * {@link java.util.Map}, may be {@code null} if the workflow produced no output).</li>
 * <li>{@code Disabled(reason)} — the workflow is currently disabled. The HTTP controller maps this to a 410 GONE; the
 * copilot bridge can map it to an AG-UI {@code RUN_ERROR} event with the reason.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public sealed interface WebhookExecutionResult {

    /**
     * Successful workflow execution. {@code outputs} is the raw workflow result — typically a map keyed by output
     * variable name. {@code null} when the workflow returns no output.
     */
    record Ok(@Nullable Object outputs) implements WebhookExecutionResult {
    }

    /**
     * The workflow was disabled at execution time. Carries a short human-readable reason for the caller to surface.
     */
    record Disabled(String reason) implements WebhookExecutionResult {
    }
}
