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
 * Audit event types emitted through {@link WorkspaceApiKeyAuditPublisher} for workspace API key binding mutations.
 *
 * @author Ivica Cardic
 */
public enum WorkspaceApiKeyAuditEvent {

    /**
     * A new workspace API key binding was persisted. Payload: {@code workspaceApiKeyId} identifies the created row.
     */
    WORKSPACE_API_KEY_CREATED,

    /**
     * A workspace API key binding was deleted. Payload: {@code workspaceApiKeyId} identifies the now-removed row.
     */
    WORKSPACE_API_KEY_DELETED
}
