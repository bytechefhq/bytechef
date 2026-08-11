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

package com.bytechef.platform.knowledgebase.audit;

/**
 * Audit event types emitted through {@link KnowledgeBaseAuditPublisher}.
 *
 * <p>
 * {@code KB_*} events describe lifecycle and workspace-assignment transitions on the knowledge-base aggregate.
 * Agent-driven knowledge-base document mutations are audited separately through the AI-hub audit publisher; this enum
 * covers knowledge-base lifecycle plus workspace assignment only.
 *
 * <p>
 * Every event carries the implicit {@code knowledgeBaseId} key that {@link KnowledgeBaseAuditPublisher} attaches.
 * Additional payload keys are documented per constant and are convention-enforced rather than type-checked, so changes
 * must be applied at every emitter.
 *
 * @author Ivica Cardic
 */
public enum KnowledgeBaseAuditEvent {

    /**
     * A new knowledge base was persisted. Payload: {@code name}.
     */
    KB_CREATED,

    /**
     * An existing knowledge base was updated. Payload: no additional keys required; {@code knowledgeBaseId} identifies
     * the updated row.
     */
    KB_UPDATED,

    /**
     * A knowledge base was deleted. Payload: no additional keys required; {@code knowledgeBaseId} identifies the
     * now-removed row.
     */
    KB_DELETED,

    /**
     * A knowledge base was assigned to a workspace. Payload: {@code workspaceId} (stringified).
     */
    KB_ASSIGNED_TO_WORKSPACE,

    /**
     * A knowledge base was removed from a workspace. Payload: {@code workspaceId} (stringified).
     */
    KB_REMOVED_FROM_WORKSPACE
}
