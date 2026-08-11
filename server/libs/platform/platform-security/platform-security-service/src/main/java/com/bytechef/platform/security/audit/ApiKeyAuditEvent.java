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

package com.bytechef.platform.security.audit;

/**
 * Audit event types emitted through {@link ApiKeyAuditPublisher} for API-key lifecycle mutations.
 *
 * @author Ivica Cardic
 */
public enum ApiKeyAuditEvent {

    /**
     * A new API key was persisted. Payload: {@code apiKeyId} (always attached) plus {@code name} and {@code type} when
     * available on the saved entity.
     */
    API_KEY_CREATED,

    /**
     * An API key was deleted. Payload: {@code apiKeyId} identifies the now-removed row.
     */
    API_KEY_DELETED
}
