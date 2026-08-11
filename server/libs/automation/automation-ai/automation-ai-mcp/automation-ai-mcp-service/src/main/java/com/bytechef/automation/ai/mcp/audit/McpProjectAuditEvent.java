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

package com.bytechef.automation.ai.mcp.audit;

/**
 * Audit event types emitted through {@link McpProjectAuditPublisher}.
 *
 * <p>
 * Each event describes a lifecycle transition on the MCP project aggregate. The {@code mcpProjectId} is attached to
 * every event by {@link McpProjectAuditPublisher}; the additional payload keys carried per event are documented below.
 *
 * @author Ivica Cardic
 */
public enum McpProjectAuditEvent {

    /**
     * A new MCP project was persisted. Payload: {@code projectId} (stringified id of the backing project).
     */
    MCP_PROJECT_CREATED,

    /**
     * An MCP project was deleted. Payload: no additional keys required; {@code mcpProjectId} identifies the now-removed
     * row.
     */
    MCP_PROJECT_DELETED
}
