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
 * Audit event types emitted through {@link ProjectAuditPublisher} for project-lifecycle mutations.
 *
 * @author Ivica Cardic
 */
public enum ProjectAuditEvent {

    /**
     * A new project was persisted. Payload: {@code projectId} identifies the created row.
     */
    PROJECT_CREATED,

    /**
     * An existing project was updated. Payload: {@code projectId} identifies the updated row.
     */
    PROJECT_UPDATED,

    /**
     * A project was deleted. Payload: {@code projectId} identifies the now-removed row.
     */
    PROJECT_DELETED,

    /**
     * The project's reach changed. Payload: {@code projectId}, {@code toVisibility} (the new {@code ResourceVisibility}
     * name).
     */
    PROJECT_VISIBILITY_CHANGED,

    /**
     * A named workspace member was granted access to a withheld project. Payload: {@code projectId},
     * {@code targetUserId}.
     */
    PROJECT_ACCESS_GRANTED,

    /**
     * A grant was revoked. Payload: {@code projectId}, {@code targetUserId}.
     */
    PROJECT_ACCESS_REVOKED
}
