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

package com.bytechef.platform.user.audit;

/**
 * Audit event types emitted through {@link UserAuditPublisher}.
 *
 * <p>
 * Each value describes a state transition on the user aggregate. {@link UserAuditPublisher} attaches the implicit
 * {@code userId} key to every event; values may carry additional payload keys (for example {@code login}) supplied by
 * the call site.
 *
 * @author Ivica Cardic
 */
public enum UserAuditEvent {

    /**
     * A new user account was persisted. Payload: optional {@code login} and {@code email}.
     */
    USER_CREATED,

    /**
     * A registration was activated. Payload: no additional keys required.
     */
    USER_ACTIVATED,

    /**
     * A user's password was changed. Payload: no additional keys required.
     */
    USER_PASSWORD_CHANGED,

    /**
     * A user account was deleted. Payload: optional {@code login}.
     */
    USER_DELETED
}
