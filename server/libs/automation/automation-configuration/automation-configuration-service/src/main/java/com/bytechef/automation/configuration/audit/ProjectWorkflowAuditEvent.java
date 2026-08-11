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

package com.bytechef.automation.configuration.audit;

/**
 * Audit event types emitted through {@link ProjectWorkflowAuditPublisher} for project-workflow-lifecycle mutations.
 *
 * @author Ivica Cardic
 */
public enum ProjectWorkflowAuditEvent {

    /**
     * A new project workflow was persisted. Payload: {@code projectWorkflowId} (always attached) plus
     * {@code workflowId} and {@code projectId} when available.
     */
    WORKFLOW_CREATED,

    /**
     * An existing project workflow was updated. Payload: {@code projectWorkflowId} (always attached) plus
     * {@code workflowId} when available.
     */
    WORKFLOW_UPDATED,

    /**
     * A project workflow was deleted. Payload: {@code projectWorkflowId} identifies the now-removed row plus
     * {@code workflowId} and {@code projectId} when available.
     */
    WORKFLOW_DELETED
}
